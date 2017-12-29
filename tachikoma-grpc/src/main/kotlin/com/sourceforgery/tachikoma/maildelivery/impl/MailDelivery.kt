package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.SentMailMessageBodyDBO
import com.sourceforgery.tachikoma.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.maildelivery.MessageStatus
import com.sourceforgery.tachikoma.maildelivery.OutgoingEmail
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver

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

        val emailSent= request.recipientsList.map {
            val recipient: Email
            val recipientName: String
            when (it.toCase) {
                EmailRecipient.ToCase.EMAIL -> {
                    recipient = Email(it.email.email!!)
                    recipientName = ""
                }
                EmailRecipient.ToCase.NAMED -> {
                    recipient = Email(it.named.email!!)
                    recipientName = it.named.name!!
                }
                else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
            }


            EmailDBO(
                    recipient = recipient,
                    recipientName = recipientName,
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )
        }
        responseObserver.onCompleted()
    }

    private fun sendTemplatedEmail(request: OutgoingEmail, responseObserver: StreamObserver<MessageStatus>) {
        val transaction = EmailSendTransactionDBO(
                jsonRequest = PRINTER.print(request)!!
        )

        request.toString()



        val template = request.template!!
        val mailMessageBody = SentMailMessageBodyDBO(
                wrapAndPackBody(request, mergeTemplate(template, ), )
        )

        val emailSent= request.recipientsList.map {
            val recipient: Email
            val recipientName: String
            when (it.toCase) {
                EmailRecipient.ToCase.EMAIL -> {
                    recipient = Email(it.email.email!!)
                    recipientName = ""
                }
                EmailRecipient.ToCase.NAMED -> {
                    recipient = Email(it.named.email!!)
                    recipientName = it.named.name!!
                }
                else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
            }


            EmailDBO(
                    recipient = recipient,
                    recipientName = recipientName,
                    transaction = transaction,
                    sentEmailMessageBodyDBO = mailMessageBody
            )
        }
        responseObserver.onCompleted()
    }




    private fun wrapAndPackBody(request: OutgoingEmail, htmlBody: String?, plaintextBody: String?): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        val PRINTER = JsonFormat.printer()!!
    }
}