package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.jersey.uribuilder.JerseyUriBuilder
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.database.TransactionManager
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailSendTransactionDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.objects.recipientNamedEmail
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Queued
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Rejected
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcRejectReason
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageIdFactory
import com.sourceforgery.tachikoma.maildelivery.getPlainText
import com.sourceforgery.tachikoma.maildelivery.inlineStyles
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeConfig
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.HashMap
import java.util.Properties
import java.util.concurrent.Executors
import javax.activation.DataHandler
import javax.inject.Inject
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.annotations.TestOnly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MailDeliveryService
@Inject
private constructor(
    private val trackingConfig: TrackingConfig,
    private val unsubscribeConfig: UnsubscribeConfig,
    private val dbObjectMapper: DBObjectMapper,
    private val emailDAO: EmailDAO,
    private val emailSendTransactionDAO: EmailSendTransactionDAO,
    private val blockedEmailDAO: BlockedEmailDAO,
    private val mqSender: MQSender,
    private val mqSequenceFactory: MQSequenceFactory,
    private val jobMessageFactory: JobMessageFactory,
    private val transactionManager: TransactionManager,
    private val trackingDecoderImpl: TrackingDecoderImpl,
    private val unsubscribeDecoderImpl: UnsubscribeDecoderImpl,
    private val incomingEmailDAO: IncomingEmailDAO,
    private val authenticationDAO: AuthenticationDAO,
    private val messageIdFactory: MessageIdFactory
) {

    fun sendEmail(
        request: OutgoingEmail,
        responseObserver: StreamObserver<EmailQueueStatus>,
        authenticationId: AuthenticationId
    ) {
        val auth = authenticationDAO.getActiveById(authenticationId)!!
        val fromEmail = request.from.toNamedEmail().address
        if (fromEmail.domain != auth.account.mailDomain) {
            throw InvalidOrInsufficientCredentialsException("${auth.account.mailDomain} is not allowed to send emails with from domain: ${fromEmail.domain}")
        }

        val transaction = EmailSendTransactionDBO(
            jsonRequest = getRequestData(request),
            fromEmail = fromEmail,
            authentication = auth,
            bcc = request.bccList.map { it.toEmail().address },
            metaData = request.trackingData.metadataMap,
            tags = request.trackingData.tagsList
        )
        val requestedSendTime =
            if (request.hasSendAt()) {
                request.sendAt.toInstant()
            } else {
                Instant.EPOCH
            }

        transactionManager.runInTransaction {
            emailSendTransactionDAO.save(transaction)

            for (recipient in request.recipientsList) {

                val recipientEmail = auth.recipientOverride
                    ?.let {
                        NamedEmail(it, "Overridden email")
                    }
                    ?: recipient.toNamedEmail()

                val blockedReason = blockedEmailDAO.getBlockedReason(
                    accountDBO = auth.account,
                    recipient = recipientEmail.address,
                    from = fromEmail
                )

                if (blockedReason != null) {
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
                    LOGGER.debug { "Email to ${recipientEmail.address} had unsubscribed emails from $fromEmail, hence bounced" }
                    // Blocked
                    continue
                }

                val messageId = messageIdFactory.createMessageId(
                    domain = fromEmail.domain
                )
                val unsubscribeDomainOverride = unsubscribeConfig.unsubscribeDomainOverride
                val autoMailId = if (unsubscribeDomainOverride == null) {
                    AutoMailId("$messageId")
                } else {
                    AutoMailId("${messageId.localPart}@$unsubscribeDomainOverride")
                }

                val emailDBO = EmailDBO(
                    recipient = recipientEmail,
                    transaction = transaction,
                    messageId = messageId,
                    metaData = recipient.metadataMap,
                    autoMailId = autoMailId
                )
                emailDAO.save(emailDBO)

                val pair = when (request.bodyCase) {
                    OutgoingEmail.BodyCase.STATIC -> getStaticBody(
                        request = request,
                        emailDBO = emailDBO
                    )
                    OutgoingEmail.BodyCase.TEMPLATE -> getTemplateBody(
                        request = request,
                        recipient = recipient,
                        emailDBO = emailDBO
                    )
                    else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
                }
                emailDBO.subject = pair.first
                emailDBO.body = pair.second
                emailDAO.save(emailDBO)
            }
        }
        val refreshedTransaction = emailSendTransactionDAO.get(transaction.id)!!
        for (emailDBO in refreshedTransaction.emails) {
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

    private fun getTemplateBody(
        request: OutgoingEmail,
        recipient: EmailRecipient,
        emailDBO: EmailDBO
    ): Pair<String, String> {
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

        val subject = mergeTemplate(template.subject, globalVars, recipientVars)
        return subject to wrapAndPackBody(
            request = request,
            htmlBody = if (request.inlineCss) {
                runCSSInline(htmlBody)
            } else {
                htmlBody
            },
            plaintextBody = plaintextBody.emptyToNull(),
            subject = subject,
            emailDBO = emailDBO
        )
    }

    private fun getStaticBody(
        request: OutgoingEmail,
        emailDBO: EmailDBO
    ): Pair<String, String> {
        val static = request.static
        val htmlBody = static.htmlBody.emptyToNull()
        val plaintextBody = static.plaintextBody.emptyToNull()

        return static.subject to wrapAndPackBody(
            request = request,
            htmlBody = if (request.inlineCss) {
                runCSSInline(htmlBody)
            } else {
                htmlBody
            },
            plaintextBody = plaintextBody,
            subject = static.subject,
            emailDBO = emailDBO
        )
    }

    private fun runCSSInline(htmlBody: String?): String? {
        return inlineStyles(htmlBody)?.html()
    }

    private fun unwrapStruct(struct: Struct): HashMap<String, Any> {
        return dbObjectMapper.objectMapper.readValue<HashMap<String, Any>>(
            JsonFormat.printer().print(struct),
            object : TypeReference<HashMap<String, Any>>() {}
        )
    }

    // Store the request for later debugging
    private fun getRequestData(request: OutgoingEmail) =
        dbObjectMapper.objectMapper.readValue(PRINTER.print(request)!!, ObjectNode::class.java)!!

    private fun mergeTemplate(
        template: String,
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
        emailDBO: EmailDBO
    ): String {
        if (htmlBody == null && plaintextBody == null) {
            throw IllegalArgumentException("Needs at least one of plaintext or html")
        }

        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        message.setFrom(request.from.toNamedEmail().toAddress())
        message.setSubject(subject, "UTF-8")
        message.setRecipients(Message.RecipientType.TO, arrayOf(emailDBO.recipientNamedEmail.toAddress()))

        if (request.replyTo.email.isNotBlank()) {
            val replyToMailDomain = request.replyTo.toEmail().domain.mailDomain
            val fromMailDomain = request.from.toNamedEmail().address.domain.mailDomain
            if (replyToMailDomain !== fromMailDomain) {
                throw IllegalArgumentException("Reply-to email domain $replyToMailDomain not same as from email domain $fromMailDomain")
            }
            message.replyTo = arrayOf(InternetAddress(request.replyTo.email))
        }

        val unsubscribeUri = createUnsubscribeOneClickPostLink(emailDBO.id, request.unsubscribeRedirectUri)
        addListAndAbuseHeaders(
            message = message,
            emailDBO = emailDBO,
            unsubscribeUri = unsubscribeUri
        )

        for ((key, value) in request.headersMap) {
            message.addHeader(key, value)
        }

        val multipart = MimeMultipart("alternative")

        val htmlDoc = Jsoup.parse(htmlBody ?: "<html><body>$plaintextBody</body></html>")
            .apply {
                outputSettings()
                    .indentAmount(0)
                    .prettyPrint(false)
            }

        replaceLinks(htmlDoc, emailDBO.id, unsubscribeUri)
        injectTrackingPixel(htmlDoc, emailDBO.id)

        val plaintextPart = MimeBodyPart()
        val plainText = getPlainText(htmlDoc)

        plaintextPart.setContent(plaintextBody ?: plainText, "text/plain; charset=utf-8")
        plaintextPart.setHeader("Content-Transfer-Encoding", "quoted-printable")
        multipart.addBodyPart(plaintextPart)

        val htmlPart = MimeBodyPart()
        htmlPart.setContent(htmlDoc.outerHtml(), "text/html; charset=utf-8")
        htmlPart.setHeader("Content-Transfer-Encoding", "quoted-printable")
        multipart.addBodyPart(htmlPart)

        for (attachment in request.attachmentsList) {
            val attachmentBody = MimeBodyPart()
            attachmentBody.dataHandler = DataHandler(ByteArrayDataSource(attachment.toByteArray(), attachment.contentType))
            if (attachment.fileName.isNotBlank()) {
                attachmentBody.fileName = attachment.fileName
            }
            multipart.addBodyPart(attachmentBody)
        }

        message.setContent(multipart)
        message.saveChanges()
        message.setHeader("Message-ID", "<${emailDBO.messageId}>")

        val result = ByteArrayOutputStream()
        message.writeTo(result)
        return Regex("^\\.$", RegexOption.MULTILINE)
            .replace(
                input = result.toString(StandardCharsets.UTF_8.name()),
                transform = { "=2E" }
            )
    }

    private fun addListAndAbuseHeaders(message: MimeMessage, emailDBO: EmailDBO, unsubscribeUri: URI) {
        // TODO Headers to set:
        // List-Help (IMPORTANT): <https://support.google.com/a/example.com/bin/topic.py?topic=25838>, <mailto:debug+help@example.com>

        val unsubscribeEmail = Email("unsub-${emailDBO.autoMailId}")
        val bounceReturnPathEmail = Email("bounce-${emailDBO.autoMailId}")
        // MUST have a valid DomainKeys Identified Mail (DKIM) signature that covers at least the List-Unsubscribe and List-Unsubscribe-Post headers
        message.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click")
        message.addHeader("List-Unsubscribe", "<mailto:$unsubscribeEmail>, <$unsubscribeUri>")
        message.addHeader("Return-Path", bounceReturnPathEmail.address)
        // TODO Abuse-email should be system-wide config parameter
        message.addHeader("X-Report-Abuse", "Please forward a copy of this message, including all headers, to abuse@${emailDBO.transaction.fromEmail.domain}")
        val abuseUrl = JerseyUriBuilder(trackingConfig.baseUrl)
            .paths("abuse/{0}")
            .build(emailDBO.autoMailId.autoMailId)
        message.addHeader("X-Report-Abuse", "You can also report abuse here: $abuseUrl")
        message.addHeader("X-Tachikoma-User", emailDBO.transaction.authentication.id.toString())
    }

    @TestOnly
    fun createUnsubscribeOneClickPostLink(emailId: EmailId, unsubscribeRedirectUri: String): URI {
        val unsubscribeData = UnsubscribeData.newBuilder()
            .setEmailId(emailId.toGrpcInternal())
            .setRedirectUrl(unsubscribeRedirectUri)
            .build()
        val unsubscribeUrl = unsubscribeDecoderImpl.createUrl(unsubscribeData)

        return JerseyUriBuilder(trackingConfig.baseUrl)
            .paths("unsubscribe/{0}")
            .build(unsubscribeUrl)
    }

    @TestOnly
    fun createUnsubscribeClickLink(emailId: EmailId, redirectUri: URI? = null): URI {
        val unsubscribeData = UnsubscribeData.newBuilder()
            .setEmailId(emailId.toGrpcInternal())
            .setRedirectUrl(redirectUri?.toString() ?: "")
            .build()
        val unsubscribeUrl = unsubscribeDecoderImpl.createUrl(unsubscribeData)

        return JerseyUriBuilder(trackingConfig.baseUrl)
            .paths("unsubscribe/{0}")
            .build(unsubscribeUrl)
    }

    @TestOnly
    fun createTrackingLink(emailId: EmailId, originalUri: String): URI {
        val trackingData = UrlTrackingData.newBuilder()
            .setEmailId(emailId.toGrpcInternal())
            .setRedirectUrl(originalUri)
            .build()
        val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

        return JerseyUriBuilder(trackingConfig.baseUrl)
            .paths("c/{0}")
            .build(trackingUrl)
    }

    private fun replaceLinks(doc: Document, emailId: EmailId, unsubscribeUri: URI) {
        val links = doc.select("a[href]")
        for (link in links) {
            val originalUri = link.attr("href")
                ?: ""
            if (originalUri == "*|UNSUB|*") {
                link.attr("href", unsubscribeUri.toString())
            } else if (originalUri.startsWith("http://") || originalUri.startsWith("https://")) {
                link.attr(
                    "href",
                    createTrackingLink(emailId, originalUri).toString()
                )
            }
        }
    }

    private fun injectTrackingPixel(doc: Document, emailId: EmailId) {
        val trackingData = UrlTrackingData.newBuilder()
            .setEmailId(emailId.toGrpcInternal())
            .build()
        val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

        val trackingUri = JerseyUriBuilder(trackingConfig.baseUrl)
            .paths("t/{0}")
            .build(trackingUrl)

        val trackingPixel = Element("img")
        trackingPixel.attr("src", trackingUri.toString())
        trackingPixel.attr("height", "1")
        trackingPixel.attr("width", "1")
        doc.body().appendChild(trackingPixel)
    }

    fun getIncomingEmails(responseObserver: StreamObserver<IncomingEmail>, authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId) {
        val future = mqSequenceFactory.listenForIncomingEmails(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
            callback = {
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
            }
        )
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

fun NamedEmail.toAddress(): InternetAddress =
    InternetAddress(address.address, name)
