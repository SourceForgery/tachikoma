package com.sourceforgery.tachikoma.maildelivery.impl

import com.github.mustachejava.DefaultMustacheFactory
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.SentMailMessageBodyDBO
import com.sourceforgery.tachikoma.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.maildelivery.MessageId
import com.sourceforgery.tachikoma.maildelivery.MessageStatus
import com.sourceforgery.tachikoma.maildelivery.OutgoingEmail
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import java.io.StringReader
import java.io.StringWriter

class DeliveryService : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase() {

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<MessageStatus>) {
        when (request.bodyCase!!) {
            OutgoingEmail.BodyCase.STATIC -> sendStaticEmail(request, responseObserver)
            OutgoingEmail.BodyCase.TEMPLATE -> sendTemplatedEmail(request, responseObserver)
            else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
        }

    }

    private fun sendStaticEmail(request: OutgoingEmail, responseObserver: StreamObserver<MessageStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = PRINTER.print(request)!!
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
                    MessageStatus.newBuilder()
                            .setId(
                                    MessageId.newBuilder().setId("foobar").build()
                            )
                            .build()
            )
        }
        responseObserver.onCompleted()
    }

    private fun sendTemplatedEmail(request: OutgoingEmail, responseObserver: StreamObserver<MessageStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = PRINTER.print(request)!!
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
                    MessageStatus.newBuilder()
                            .setId(
                                    MessageId.newBuilder().setId("foobar").build()
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
        TODO("not implemented. Should merge html, plaintext and basically create the email body WITH headers") //To change body of created functions use File | Settings | File Templates.
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