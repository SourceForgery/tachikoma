package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.identifiers.EmailId
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

    override fun emailNotified(request: MTAQueuedNotification, responseObserver: StreamObserver<Empty>) {
        super.emailNotified(request, responseObserver)
    }

    override fun getEmails(request: Empty, responseObserver: StreamObserver<EmailMessage>) {
        val future = mqSequenceFactory.listenForOutgoingEmails {
            val emails = emailDAO.fetchEmailData(EmailId.fromList(it.emailIdList), SentMailMessageBodyId(it.sentMailMessageBodyId))
            if (emails.isEmpty()) {
                LOGGER.warn { "Nothing found when looking trying to send email with ids: " + it.emailIdList }
            } else {

                val firstEmail = emails[0]
                val response = EmailMessage.newBuilder()
                        .setBody(firstEmail.sentMailMessageBody.body)
                        .setFrom(firstEmail.transaction.fromEmail.address)
                        .addAllEmailAddresses(emails.map { it.recipient.address })
                        .build()
                responseObserver.onNext(response)
            }
        }
        future.addListener(Runnable {
            responseObserver.onCompleted()
        }, responseCloser)
        future.get()
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<Empty>) {
        super.incomingEmail(request, responseObserver)
    }

    companion object {
        val LOGGER = logger()
    }
}