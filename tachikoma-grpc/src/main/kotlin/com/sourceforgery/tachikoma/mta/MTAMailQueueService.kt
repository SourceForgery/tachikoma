package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId
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
            val emails = emailDAO.fetchEmailData(EmailId.fromList(it.emailIdList), SentMailMessageBodyId(it.sentMailMessageBodyId))
            if (emails.isEmpty()) {
                LOGGER.warn { "Nothing found when looking trying to send email with ids: " + it.emailIdList }
            } else {

                val firstEmail = emails[0]
                val response = EmailMessage.newBuilder()
                        .setBody(firstEmail.sentMailMessageBody.body)
                        .setFrom(firstEmail.transaction.fromEmail.address)
                        .setEmailTransactionId(firstEmail.transaction.id.emailTransactionId)
                        .addAllEmailAddresses(emails.map { it.recipient.address })
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
                val emailTransactionId = EmailTransactionId(value.emailTransactionId)
                // TODO do something with value.success
                emailDAO.updateMTAQueueStatus(emailTransactionId, queueId)
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