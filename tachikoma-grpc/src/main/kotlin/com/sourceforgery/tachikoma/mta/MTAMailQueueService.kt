package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.IncomingEmailNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import io.grpc.stub.StreamObserver
import java.util.Properties
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

internal class MTAEmailQueueService
@Inject
private constructor(
        private val mqSequenceFactory: MQSequenceFactory,
        private val emailDAO: EmailDAO,
        private val incomingEmailDAO: IncomingEmailDAO,
        private val emailStatusEventDAO: EmailStatusEventDAO,
        private val blockedEmailDAO: BlockedEmailDAO,
        private val mqSender: MQSender,
        private val incomingEmailAddressDAO: IncomingEmailAddressDAO
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
        try {
            val body = request.body.toByteArray()
            val fromEmail = Email(InternetAddress(request.from).address)
            val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), body.inputStream())
            val recipientEmail = Email(InternetAddress(request.emailAddress).address)
            val accountTypePair = handleUnsubscribe(recipientEmail, mimeMessage)
                    ?: handleHardBounce(recipientEmail)
                    ?: handleNormalEmails(recipientEmail)

            if (accountTypePair != null) {
                val accountDBO = accountTypePair.first
                val incomingEmailDBO = IncomingEmailDBO(
                        body = body,
                        fromEmail = fromEmail,
                        receiverEmail = recipientEmail,
                        accountDBO = accountDBO
                )
                incomingEmailDAO.save(incomingEmailDBO)
                if (accountTypePair.second == IncomingEmailType.NORMAL) {
                    val notificationMessage = IncomingEmailNotificationMessage.newBuilder()
                            .setIncomingEmailMessageId(incomingEmailDBO.id.incomingEmailId)
                            .build()
                    mqSender.queueIncomingEmailNotification(accountDBO.id, notificationMessage)
                }
            } else {
                TODO("Return bad result!")
            }
        } catch (e: Exception) {
            responseObserver.onError(e)
        }
        responseObserver.onCompleted()
    }

    private fun handleNormalEmails(recipientEmail: Email): Pair<AccountDBO, IncomingEmailType>? {
        return incomingEmailAddressDAO.getByEmail(recipientEmail)
                ?.let {
                    it.account to IncomingEmailType.NORMAL
                }
    }

    private fun handleHardBounce(recipientAddress: Email): Pair<AccountDBO, IncomingEmailType>? {
        return if (recipientAddress.address.startsWith("bounce-")) {
            val messageId = MessageId(recipientAddress.address.substringAfter('-'))
            emailDAO.getByMessageId(messageId)
                    ?.let { email ->
                        val emailStatusEventDBO = EmailStatusEventDBO(
                                email = email,
                                emailStatus = EmailStatus.HARD_BOUNCED
                        )
                        emailStatusEventDAO.save(emailStatusEventDBO)
                        blockedEmailDAO.block(emailStatusEventDBO)
                        email.transaction.authentication.account!! to IncomingEmailType.HARD_BOUNCE
                    }
        } else {
            null
        }
    }

    private fun handleUnsubscribe(recipientAddress: Email, mimeMessage: MimeMessage): Pair<AccountDBO, IncomingEmailType>? {
        return if (recipientAddress.address.startsWith("unsub-") && mimeMessage.subject.startsWith(prefix = "unsub", ignoreCase = true)) {
            val messageId = MessageId(recipientAddress.address.substringAfter('-'))
            emailDAO.getByMessageId(messageId)
                    ?.let { email ->
                        val emailStatusEventDBO = EmailStatusEventDBO(
                                email = email,
                                emailStatus = EmailStatus.UNSUBSCRIBE
                        )
                        emailStatusEventDAO.save(emailStatusEventDBO)
                        blockedEmailDAO.block(emailStatusEventDBO)
                        email.transaction.authentication.account!! to IncomingEmailType.UNSUBSCRIBE
                    }
        } else {
            null
        }
    }

    companion object {
        val LOGGER = logger()
    }
}

enum class IncomingEmailType {
    UNSUBSCRIBE,
    HARD_BOUNCE,
    NORMAL
}
