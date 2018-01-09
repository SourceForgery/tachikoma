package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.SentMailMessageBodyDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Queued
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.mq.MQSender
import io.ebean.EbeanServer
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import net.htmlparser.jericho.Source
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Properties
import javax.inject.Inject
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

internal class MailDeliveryService
@Inject
private constructor(
        private val dbObjectMapper: DBObjectMapper,
        private val emailDAO: EmailDAO,
        private val mqSender: MQSender,
        private val jobMessageFactory: JobMessageFactory,
        private val ebeanServer: EbeanServer
) : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase() {

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        when (request.bodyCase!!) {
            OutgoingEmail.BodyCase.STATIC -> sendStaticEmail(request, responseObserver)
            OutgoingEmail.BodyCase.TEMPLATE -> sendTemplatedEmail(request, responseObserver)
            else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
        }
    }

    private fun sendStaticEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = getRequestData(request),
                fromEmail = request.from.toNamedEmail().address
        )

        val static = request.static!!
        val mailMessageBody = SentMailMessageBodyDBO(
                body = wrapAndPackBody(request, static.htmlBody.emptyToNull(), static.plaintextBody.emptyToNull(), static.subject)
        )
        val requestedSendTime =
                if (request.hasSendAt()) {
                    request.sendAt.toInstant()
                } else {
                    Instant.EPOCH
                }

        ebeanServer.createTransaction().use {
            val emailIds = LinkedHashMap<EmailId, Email>()
            for (recipient in request.recipientsList) {
                // TODO Check if recipient is blocked

                val emailDBO = EmailDBO(
                        recipient = recipient.toNamedEmail(),
                        transaction = transaction,
                        sentMailMessageBody = mailMessageBody
                )
                emailDAO.save(emailDBO)
                emailIds[emailDBO.id] = emailDBO.recipient
            }

            mqSender.queueJob(jobMessageFactory.createSendEmailJob(
                    requestedSendTime = requestedSendTime,
                    sentMailMessageBodyId = mailMessageBody.id,
                    emailIds = emailIds.keys
            ))
            val grpcEmailTransactionId = transaction.id.toGrpcInternal()
            for ((emailId, recipient) in emailIds) {
                responseObserver.onNext(
                        EmailQueueStatus.newBuilder()
                                .setEmailId(emailId.toGrpcInternal())
                                .setQueued(Queued.getDefaultInstance())
                                .setTransactionId(grpcEmailTransactionId)
                                .setRecipient(recipient.toGrpcInternal())
                                .build()
                )
            }
        }
        responseObserver.onCompleted()
    }

    private fun sendTemplatedEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        val template = request.template!!
        if (template.htmlTemplate == null && template.plaintextTemplate == null) {
            throw IllegalArgumentException("Needs at least one template (plaintext or html)")
        }

        val transaction = EmailSendTransactionDBO(
                jsonRequest = getRequestData(request),
                fromEmail = request.from.toNamedEmail().address
        )
        val requestedSendTime =
                if (request.hasSendAt()) {
                    request.sendAt.toInstant()
                } else {
                    Instant.EPOCH
                }

        val globalVarsStruct =
                if (template.hasGlobalVars()) {
                    template.globalVars
                } else {
                    Struct.getDefaultInstance()
                }
        val globalVars = unwrapStruct(globalVarsStruct)

        ebeanServer.createTransaction().use {
            for (recipient in request.recipientsList) {
                // TODO Check if recipient is blocked
                val recipientVars = unwrapStruct(recipient.templateVars)
                val htmlBody = mergeTemplate(template.htmlTemplate, globalVars, recipientVars)
                val plaintextBody = mergeTemplate(template.plaintextTemplate, globalVars, recipientVars)
                val mailMessageBody = SentMailMessageBodyDBO(
                        wrapAndPackBody(
                                request = request,
                                htmlBody = htmlBody.emptyToNull(),
                                plaintextBody = plaintextBody.emptyToNull(),
                                subject = mergeTemplate(template.subject, globalVars, recipientVars)
                        )
                )
                val emailDBO = EmailDBO(
                        recipient = recipient.toNamedEmail(),
                        transaction = transaction,
                        sentMailMessageBody = mailMessageBody
                )
                emailDAO.save(emailDBO)

                mqSender.queueJob(jobMessageFactory.createSendEmailJob(
                        requestedSendTime = requestedSendTime,
                        sentMailMessageBodyId = mailMessageBody.id,
                        emailIds = listOf(emailDBO.id)
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
            responseObserver.onCompleted()
        }
    }

    private fun unwrapStruct(struct: Struct): HashMap<String, Any> {
        val objectMapper = ObjectMapper().registerKotlinModule()
        return objectMapper.readValue<HashMap<String, Any>>(
                JsonFormat.printer().print(struct),
                object : TypeReference<HashMap<String, Any>>() {}
        )
    }

    // Store the request for later debugging
    private fun getRequestData(request: OutgoingEmail) =
            dbObjectMapper.readValue(PRINTER.print(request)!!, ObjectNode::class.java)!!

    private fun mergeTemplate(template: String?, vararg scopes: HashMap<String, Any>) =
            StringWriter().use {
                DefaultMustacheFactory()
                        .compile(StringReader(template), "html")
                        .execute(it, scopes)
                it.toString()
            }

    private fun wrapAndPackBody(request: OutgoingEmail, htmlBody: String?, plaintextBody: String?, subject: String): String {
        val plainTextString = plaintextBody
                ?: htmlBody?.let { stripHtml(it) }
                ?: throw IllegalArgumentException("Needs at least one of plaintext or html")

        // TODO add headers here
        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(request.from.email, request.from.name))
        message.subject = subject

        for ((key, value) in request.headersMap) {
            message.addHeader(key, value)
        }

        val multipart = MimeMultipart("alternative")

        val plaintextPart = MimeBodyPart()
        plaintextPart.setContent(plainTextString, "text/plain")
        multipart.addBodyPart(plaintextPart)

        if (htmlBody != null) {
            val htmlPart = MimeBodyPart()
            htmlPart.setContent(htmlBody, "text/html")
            multipart.addBodyPart(htmlPart)
        }

        message.setContent(multipart)

        val result = ByteArrayOutputStream()
        message.writeTo(result)
        return result.toString(StandardCharsets.UTF_8.name())
    }

    private fun stripHtml(html: String) =
            Source(html).textExtractor.toString()

    companion object {
        val PRINTER = JsonFormat.printer()!!
    }
}
