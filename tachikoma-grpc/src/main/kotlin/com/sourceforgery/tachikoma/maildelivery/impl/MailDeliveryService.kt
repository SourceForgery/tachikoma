@file:Suppress("UNUSED_VARIABLE")

package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.SentMailMessageBodyDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Queued
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.mq.JobFactory
import com.sourceforgery.tachikoma.mq.MQSender
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import javax.inject.Inject

internal class MailDeliveryService
@Inject
private constructor(
        private val dbObjectMapper: DBObjectMapper,
        private val emailDAO: EmailDAO,
        private val mqSender: MQSender,
        private val jobFactory: JobFactory
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
                jsonRequest = getRequestData(request)
        )
        val transactionId = transaction.id.toGrpcInternal()

        val static = request.static!!
        val mailMessageBody = SentMailMessageBodyDBO(
                body = wrapAndPackBody(request, static.htmlBody, static.plaintextBody)
        )
        val requestedSendTime =
                if (request.hasSendAt()) {
                    request.sendAt.toInstant()
                } else {
                    Instant.EPOCH
                }

        val emailSent = request.recipientsList.map {
            // TODO Check if recipient is blocked

            val emailDBO = EmailDBO(
                    recipient = it.toNamedEmail(),
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )
            mqSender.queueJob(jobFactory.createSendEmailJob(
                    requestedSendTime = requestedSendTime,
                    emailId = emailDBO.id
            ))
            emailDAO.save(emailDBO)
            responseObserver.onNext(
                    EmailQueueStatus.newBuilder()
                            .setEmailId(
                                    emailDBO.id.toGrpcInternal()
                            )
                            .setQueued(Queued.getDefaultInstance())
                            .setTransactionId(transactionId)
                            .setRecipient(emailDBO.recipient.toGrpcInternal())
                            .build()
            )
        }
        responseObserver.onCompleted()
    }

    private fun sendTemplatedEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        val template = request.template!!
        if (template.htmlTemplate == null && template.plaintextTemplate == null) {
            throw IllegalArgumentException("Needs at least one template (plaintext or html)")
        }

        val transaction = EmailSendTransactionDBO(
                jsonRequest = getRequestData(request)
        )
        val transactionId = transaction.id.toGrpcInternal()
        val requestedSendTime =
                if (request.hasSendAt()) {
                    request.sendAt.toInstant()
                } else {
                    Instant.EPOCH
                }

        val globalVars: Struct =
                if (template.hasGlobalVars()) {
                    template.globalVars
                } else {
                    Struct.getDefaultInstance()
                }
        val emailSent = request.recipientsList.map {
            // TODO Check if recipient is blocked
            val templateVars = it.templateVars ?: Struct.getDefaultInstance()
            val htmlBody = mergeTemplate(template.htmlTemplate, globalVars, templateVars)
            val plaintextBody = mergeTemplate(template.plaintextTemplate, globalVars, templateVars)
            val mailMessageBody = SentMailMessageBodyDBO(
                    wrapAndPackBody(
                            request = request,
                            htmlBody = htmlBody,
                            plaintextBody = plaintextBody
                    )
            )
            val emailDBO = EmailDBO(
                    recipient = it.toNamedEmail(),
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )

            emailDAO.save(emailDBO)
            mqSender.queueJob(jobFactory.createSendEmailJob(
                    requestedSendTime = requestedSendTime,
                    emailId = emailDBO.id
            ))

            responseObserver.onNext(
                    EmailQueueStatus.newBuilder()
                            .setEmailId(
                                    emailDBO.id.toGrpcInternal()
                            )
                            .setQueued(Queued.getDefaultInstance())
                            .setTransactionId(transactionId)
                            .setRecipient(emailDBO.recipient.toGrpcInternal())
                            .build()
            )
        }
        responseObserver.onCompleted()
    }

    // Store the request for later debugging
    private fun getRequestData(request: OutgoingEmail) =
            dbObjectMapper.convertValue(PRINTER.print(request)!!, ObjectNode::class.java)!!

    private fun mergeTemplate(template: String?, vararg scopes: Any) =
            StringWriter().use {
                DefaultMustacheFactory()
                        .compile(StringReader(template), "html")
                        .execute(it, scopes)
                it.toString()
            }

    private fun wrapAndPackBody(request: OutgoingEmail, htmlBody: String?, plaintextBody: String?): String {
        TODO("not implemented. Should merge html, plaintext and basically create the email body WITH headers")
    }

    companion object {
        val PRINTER = JsonFormat.printer()!!
    }
}