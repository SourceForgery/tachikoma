package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Queued
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Rejected
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcRejectReason
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import io.ebean.EbeanServer
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import net.moznion.uribuildertiny.URIBuilderTiny
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Whitelist
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Properties
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

internal class MailDeliveryService
@Inject
private constructor(
        private val trackingConfig: TrackingConfig,
        private val dbObjectMapper: DBObjectMapper,
        private val emailDAO: EmailDAO,
        private val blockedEmailDAO: BlockedEmailDAO,
        private val mqSender: MQSender,
        private val mqSequenceFactory: MQSequenceFactory,
        private val jobMessageFactory: JobMessageFactory,
        private val ebeanServer: EbeanServer,
        private val trackingDecoderImpl: TrackingDecoderImpl,
        private val unsubscribeDecoderImpl: UnsubscribeDecoderImpl,
        private val authentication: Authentication,
        private val incomingEmailDAO: IncomingEmailDAO,
        private val authenticationDAO: AuthenticationDAO
) {

    fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        authentication.requireFrontend()
        val auth = authenticationDAO.getActiveById(authentication.authenticationId)!!
        val fromEmail = request.from.toNamedEmail().address
        if (fromEmail.domain != auth.account.mailDomain) {
            throw InvalidOrInsufficientCredentialsException()
        }
        val transaction = EmailSendTransactionDBO(
                jsonRequest = getRequestData(request),
                fromEmail = fromEmail,
                authentication = auth
        )
        val requestedSendTime =
                if (request.hasSendAt()) {
                    request.sendAt.toInstant()
                } else {
                    Instant.EPOCH
                }

        ebeanServer.createTransaction().use {
            for (recipient in request.recipientsList) {

                val recipientEmail = auth.recipientOverride
                        ?. let {
                            NamedEmail(it, "Overriden email")
                        }
                        ?: recipient.toNamedEmail()

                blockedEmailDAO.getBlockedReason(recipient = recipientEmail.address, from = fromEmail)
                        ?.let { blockedReason ->
                            responseObserver.onNext(
                                    EmailQueueStatus.newBuilder()
                                            .setRejected(Rejected.newBuilder()
                                                    .setRejectReason(blockedReason.toGrpcRejectReason())
                                                    .build()
                                            )
                                            .setTransactionId(transaction.id.toGrpcInternal())
                                            .setRecipient(recipientEmail.address.toGrpcInternal())
                                            .build()
                            )
                            blockedReason
                        }
                        ?: let {
                            val messageId = MessageId("${UUID.randomUUID()}@${fromEmail.domain}")
                            val emailDBO = EmailDBO(
                                    recipient = recipientEmail,
                                    transaction = transaction,
                                    messageId = messageId
                            )
                            emailDAO.save(emailDBO)

                            emailDBO.body = when (request.bodyCase) {
                                OutgoingEmail.BodyCase.STATIC -> getStaticBody(
                                        request = request,
                                        emailId = emailDBO.id,
                                        messageId = messageId,
                                        fromEmail = fromEmail
                                )
                                OutgoingEmail.BodyCase.TEMPLATE -> getTemplateBody(
                                        request = request,
                                        recipient = recipient,
                                        emailId = emailDBO.id,
                                        messageId = messageId,
                                        fromEmail = fromEmail
                                )
                                else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
                            }
                            emailDAO.save(emailDBO)

                            mqSender.queueJob(jobMessageFactory.createSendEmailJob(
                                    requestedSendTime = requestedSendTime,
                                    emailId = emailDBO.id,
                                    mailDomain = auth.account.mailDomain
                            ))

                            responseObserver.onNext(
                                    EmailQueueStatus.newBuilder()
                                            .setEmailId(emailDBO.id.toGrpcInternal())
                                            .setQueued(Queued.getDefaultInstance())
                                            .setTransactionId(transaction.id.toGrpcInternal())
                                            .setRecipient(emailDBO.recipient.toGrpcInternal())
                                            .build()
                            )
                        }
            }
        }
    }

    private fun getTemplateBody(
            request: OutgoingEmail,
            recipient: EmailRecipient,
            emailId: EmailId,
            messageId: MessageId,
            fromEmail: Email
    ): String {
        val template = request.template
        if (template.htmlTemplate.isBlank() && template.plaintextTemplate.isBlank()) {
            throw IllegalArgumentException("Needs at least one template (plaintext or html)")
        }

        val globalVarsStruct =
                if (template.hasGlobalVars()) {
                    template.globalVars
                } else {
                    Struct.getDefaultInstance()
                }
        val globalVars = unwrapStruct(globalVarsStruct)

        val recipientVars = unwrapStruct(recipient.templateVars)

        val htmlBody = mergeTemplate(template.htmlTemplate, globalVars, recipientVars)
        val plaintextBody = mergeTemplate(template.plaintextTemplate, globalVars, recipientVars)

        return wrapAndPackBody(
                request = request,
                htmlBody = htmlBody.emptyToNull(),
                plaintextBody = plaintextBody.emptyToNull(),
                subject = mergeTemplate(template.subject, globalVars, recipientVars),
                emailId = emailId,
                messageId = messageId,
                fromEmail = fromEmail
        )
    }

    private fun getStaticBody(
            request: OutgoingEmail,
            emailId: EmailId,
            messageId: MessageId,
            fromEmail: Email
    ): String {
        val static = request.static
        val htmlBody = static.htmlBody.emptyToNull()
        val plaintextBody = static.plaintextBody.emptyToNull()

        return wrapAndPackBody(
                request = request,
                htmlBody = htmlBody,
                plaintextBody = plaintextBody,
                subject = static.subject,
                emailId = emailId,
                messageId = messageId,
                fromEmail = fromEmail
        )
    }

    private fun unwrapStruct(struct: Struct): HashMap<String, Any> {
        return dbObjectMapper.readValue<HashMap<String, Any>>(
                JsonFormat.printer().print(struct),
                object : TypeReference<HashMap<String, Any>>() {}
        )
    }

    // Store the request for later debugging
    private fun getRequestData(request: OutgoingEmail) =
            dbObjectMapper.readValue(PRINTER.print(request)!!, ObjectNode::class.java)!!

    private fun mergeTemplate(
            template: String?,
            vararg scopes: HashMap<String, Any>
    ) = StringWriter().use {
        DefaultMustacheFactory()
                .compile(StringReader(template), "html")
                .execute(it, scopes)
        it.toString()
    }

    private fun wrapAndPackBody(
            request: OutgoingEmail,
            htmlBody: String?,
            plaintextBody: String?,
            subject: String,
            emailId: EmailId,
            messageId: MessageId,
            fromEmail: Email
    ): String {
        if (htmlBody == null && plaintextBody == null) {
            throw IllegalArgumentException("Needs at least one of plaintext or html")
        }

        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(request.from.email, request.from.name))
        message.subject = subject

        addListAndAbuseHeaders(message, emailId, messageId, fromEmail)

        for ((key, value) in request.headersMap) {
            message.addHeader(key, value)
        }

        val multipart = MimeMultipart("alternative")

        val htmlDoc = Jsoup.parse(htmlBody ?: "<html><body>$plaintextBody</body></html>")

        replaceLinks(htmlDoc, emailId)
        injectTrackingPixel(htmlDoc, emailId)

        val htmlPart = MimeBodyPart()
        htmlPart.setContent(htmlDoc.outerHtml(), "text/html")
        multipart.addBodyPart(htmlPart)

        val plaintextPart = MimeBodyPart()
        val plainText = getPlainText(htmlDoc)

        plaintextPart.setContent(plaintextBody ?: plainText, "text/plain")
        multipart.addBodyPart(plaintextPart)

        message.setContent(multipart)

        val result = ByteArrayOutputStream()
        message.writeTo(result)
        return result.toString(StandardCharsets.UTF_8.name())
    }

    private fun addListAndAbuseHeaders(message: MimeMessage, emailId: EmailId, messageId: MessageId, fromEmail: Email) {
        // TODO Headers to set:
        // List-Help (IMPORTANT): <https://support.google.com/a/example.com/bin/topic.py?topic=25838>, <mailto:debug+help@example.com>

        val unsubscribeData = UnsubscribeData.newBuilder()
                .setEmailId(emailId.toGrpcInternal())
                .build()
        val unsubscribeUrl = unsubscribeDecoderImpl.createUrl(unsubscribeData)

        val unsubscribeUri = URIBuilderTiny(trackingConfig.baseUrl)
                .appendPaths("unsubscribe", unsubscribeUrl)
                .build()

        val unsubscribeEmail = Email("unsub-$messageId")
        val bounceReturnPathEmail = Email("bounce-$messageId")

        // MUST have a valid DomainKeys Identified Mail (DKIM) signature that covers at least the List-Unsubscribe and List-Unsubscribe-Post headers
        message.addHeader("List-Unsubscribe-Post", "One-Click")
        message.addHeader("List-Unsubscribe", "<$unsubscribeUri>, <mailto:$unsubscribeEmail?subject=unsub>")
        message.addHeader("Return-Path", bounceReturnPathEmail.address)
        message.addHeader("X-Report-Abuse", "Please forward a copy of this message, including all headers, to abuse@${fromEmail.domain}")
        // TODO Add this url (abuse)
        message.addHeader("X-Report-Abuse", "You can also report abuse here: http://${trackingConfig.baseUrl}/abuse/$messageId")
        message.addHeader("X-Tachikoma-User", authentication.accountId.accountId.toString())
    }

    private fun getPlainText(doc: Document): String {
        // Keeps some structure in the plain text mail version, removes all html tags and keeps indentation and line breaks
        return Jsoup
                .clean(doc.html(), "", Whitelist.none(), Document.OutputSettings().prettyPrint(false))
                .trim()
    }

    private fun replaceLinks(doc: Document, emailId: EmailId) {
        val links = doc.select("a[href]")
        links.forEach({
            val trackingData = UrlTrackingData.newBuilder()
                    .setEmailId(emailId.toGrpcInternal())
                    .setRedirectUrl(it.attr("href"))
                    .build()
            val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

            val trackingUri = URIBuilderTiny(trackingConfig.baseUrl)
                    .appendPaths("c", trackingUrl)
                    .build()

            it.attr("href", trackingUri.toString())
        })
    }

    private fun injectTrackingPixel(doc: Document, emailId: EmailId) {
        val trackingData = UrlTrackingData.newBuilder()
                .setEmailId(emailId.toGrpcInternal())
                .build()
        val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

        val trackingUri = URIBuilderTiny(trackingConfig.baseUrl)
                .appendPaths("t", trackingUrl)
                .build()

        val trackingPixel = Element("img")
        trackingPixel.attr("src", trackingUri.toString())
        trackingPixel.attr("height", "1")
        trackingPixel.attr("width", "1")
        doc.body().appendChild(trackingPixel)
    }

    fun getIncomingEmails(responseObserver: StreamObserver<IncomingEmail>) {
        authentication.requireFrontend()
        val future = mqSequenceFactory.listenForIncomingEmails(authentication.authenticationId, {
            val incomingEmailId = IncomingEmailId(it.incomingEmailMessageId)
            val email = incomingEmailDAO.fetchIncomingEmail(incomingEmailId)
            if (email != null) {
                val incomingEmail = IncomingEmail.newBuilder()
                        .setIncomingEmailId(incomingEmailId.toGrpc())
                        .setSubject(email.subject)
                        .setTo(NamedEmail(email.receiverEmail, email.receiverName).toGrpc())
                        .setFrom(NamedEmail(email.fromEmail, email.fromName).toGrpc())
                        .build()
                responseObserver.onNext(incomingEmail)
            } else {
                LOGGER.warn { "Could not find email with id $incomingEmailId" }
            }
        })
        future.addListener(Runnable {
            responseObserver.onCompleted()
        }, responseCloser)
    }

    companion object {
        private val LOGGER = logger()
        private val PRINTER = JsonFormat.printer()!!
        private val responseCloser = Executors.newCachedThreadPool()
    }
}
