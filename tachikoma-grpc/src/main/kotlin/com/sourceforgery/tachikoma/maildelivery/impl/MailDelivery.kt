@file:Suppress("UNUSED_VARIABLE")

package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.SentMailMessageBodyDBO
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.EmailMessageId
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.QueueStatus
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.io.StringReader
import java.io.StringWriter
import javax.inject.Inject

internal class DeliveryService
@Inject
private constructor(
        private val dbObjectMapper: DBObjectMapper
) : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase() {

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<QueueStatus>) {
        when (request.bodyCase!!) {
            OutgoingEmail.BodyCase.STATIC -> sendStaticEmail(request, responseObserver)
            OutgoingEmail.BodyCase.TEMPLATE -> sendTemplatedEmail(request, responseObserver)
            else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
        }
        TODO("Save all objects and actually send them on queue etc")
    }

    private fun sendStaticEmail(request: OutgoingEmail, responseObserver: StreamObserver<QueueStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = dbObjectMapper.convertValue(PRINTER.print(request)!!, ObjectNode::class.java)
        )

        request.toString()

        val static = request.static!!
        val mailMessageBody = SentMailMessageBodyDBO(
                wrapAndPackBody(request, static.htmlBody, static.plaintextBody)
        )

        val emailSent = request.recipientsList.map {
            EmailDBO(
                    recipient = it.toNamedEmail(),
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )
            responseObserver.onNext(
                    QueueStatus.newBuilder()
                            .setId(
                                    EmailMessageId.newBuilder().setId(100).build()
                            )
                            .build()
            )
        }
        responseObserver.onCompleted()
    }

    private fun sendTemplatedEmail(request: OutgoingEmail, responseObserver: StreamObserver<QueueStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = dbObjectMapper.convertValue(PRINTER.print(request)!!, ObjectNode::class.java)
        )

        val template = request.template!!
        val emailSent = request.recipientsList.map {
            val mailMessageBody = SentMailMessageBodyDBO(
                    wrapAndPackBody(
                            request = request,
                            htmlBody = mergeTemplate(template.htmlTemplate, template.globalMergeVars, it.templateMergeVars),
                            plaintextBody = mergeTemplate(template.plaintextTemplate, template.globalMergeVars, it.templateMergeVars)
                    )
            )
            EmailDBO(
                    recipient = it.toNamedEmail(),
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )
            responseObserver.onNext(
                    QueueStatus.newBuilder()
                            .setId(
                                    EmailMessageId.newBuilder().setId(100).build()
                            )
                            .build()
            )
        }
        responseObserver.onCompleted()
    }

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

internal fun EmailRecipient.toNamedEmail() =
        when (toCase) {
            EmailRecipient.ToCase.EMAIL -> NamedEmail(
                    address = email.email!!,
                    name = ""
            )
            EmailRecipient.ToCase.NAMEDEMAIL -> NamedEmail(
                    address = namedEmail.email!!,
                    name = namedEmail.name!!
            )
            else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
        }