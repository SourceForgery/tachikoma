package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import io.grpc.stub.StreamObserver
import java.util.concurrent.Executors
import javax.inject.Inject

internal class MTAEmailQueueService
@Inject
private constructor(
        private val mqSequenceFactory: MQSequenceFactory,
        private val emailDAO: EmailDAO
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    private val responseCloser = Executors.newCachedThreadPool()

    override fun getEmails(responseObserver: StreamObserver<EmailMessage>): StreamObserver<MTAQueuedNotification> {
        val future = mqSequenceFactory.listenForOutgoingEmails {
            val email = emailDAO.fetchEmailData(EmailId(it.emailId))
            if (email == null) {
                LOGGER.warn { "Nothing found when looking trying to send email with id: " + it.emailId }
            } else {
                LOGGER.info { "Email with id ${email.id} is about to be sent" }
                val response = EmailMessage.newBuilder()
                        .setBody(email.body)
                        .setFrom(email.transaction.fromEmail.address)
                        .setEmailId(email.id.emailId)
                        .setEmailAddress(email.recipient.address)
                        .build()
                responseObserver.onNext(response)
            }
        }
        future.addListener(Runnable {
            responseObserver.onCompleted()
        }, responseCloser)

        return object : StreamObserver<MTAQueuedNotification> {
            override fun onCompleted() {
                future.cancel(true)
            }

            override fun onNext(value: MTAQueuedNotification) {
                val queueId = value.queueId!!
                val emailId = EmailId(value.emailId)
                // TODO do something with value.success
                if (value.success) {
                    emailDAO.updateMTAQueueStatus(emailId, queueId)
                } else {
                    TODO("We failed for message ${value.emailId}")
                }
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
                future.cancel(true)
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<Empty>) {
        super.incomingEmail(request, responseObserver)
    }

    companion object {
        val LOGGER = logger()
    }
}
