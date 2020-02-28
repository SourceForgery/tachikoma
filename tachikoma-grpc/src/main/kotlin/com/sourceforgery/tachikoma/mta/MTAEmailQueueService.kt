package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.IncomingEmailNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.MessageHardBounced
import com.sourceforgery.tachikoma.mq.MessageQueued
import com.sourceforgery.tachikoma.mq.MessageUnsubscribed
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.time.Clock
import java.util.Properties
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import org.apache.logging.log4j.kotlin.logger

class MTAEmailQueueService
@Inject
private constructor(
    private val clock: Clock,
    private val mqSequenceFactory: MQSequenceFactory,
    private val emailDAO: EmailDAO,
    private val incomingEmailDAO: IncomingEmailDAO,
    private val emailStatusEventDAO: EmailStatusEventDAO,
    private val blockedEmailDAO: BlockedEmailDAO,
    private val mqSender: MQSender,
    private val incomingEmailAddressDAO: IncomingEmailAddressDAO
) {
    fun getEmails(responseObserver: StreamObserver<EmailMessage>, mailDomain: MailDomain): StreamObserver<MTAQueuedNotification> {
        LOGGER.info { "MTA connected with mail domain $mailDomain " }
        val serverCallStreamObserver = responseObserver as? ServerCallStreamObserver
        val future = mqSequenceFactory.listenForOutgoingEmails(mailDomain) {
            val email = emailDAO.fetchEmailData(EmailId(it.emailId))
            if (email == null) {
                LOGGER.warn { "Nothing found when looking trying to send email with id: " + it.emailId }
            } else {
                LOGGER.info { "Email with id ${email.id} is about to be sent" }
                val response = EmailMessage.newBuilder()
                    .setBody(email.body!!)
                    .setFrom(email.transaction.fromEmail.address)
                    .setEmailId(email.id.emailId)
                    .setEmailAddress(email.recipient.address)
                    .addAllBcc(email.transaction.bcc)
                    .build()
                responseObserver.onNext(response)
            }
        }
        future.addListener(Runnable {
            val cancelled = serverCallStreamObserver?.isCancelled ?: true
            if (!cancelled) {
                responseObserver.onCompleted()
            }
        }, responseCloser)

        return object : StreamObserver<MTAQueuedNotification> {
            override fun onCompleted() {
                future.cancel(true)
            }

            override fun onNext(value: MTAQueuedNotification) {
                val queueId = value.queueId
                val emailId = EmailId(value.emailId)
                val email = emailDAO.getByEmailId(emailId)
                if (email == null) {
                    LOGGER.warn { "Didn't find id for email with emailId: $emailId" }
                    return
                }

                if (value.success) {
                    check(queueId.isNotBlank()) {
                        "No queueId set in spite of successful delivery of email $emailId"
                    }
                    LOGGER.debug { "Successfully delivered email with id $emailId and mtaQueueId $queueId" }
                    email.mtaQueueId = queueId
                    emailDAO.save(email)

                    val statusDBO = EmailStatusEventDBO(
                        email = email,
                        emailStatus = EmailStatus.QUEUED,
                        metaData = StatusEventMetaData()
                    )
                    emailStatusEventDAO.save(statusDBO)
                    mqSender.queueDeliveryNotification(
                        accountId = email.transaction.authentication.account.id,
                        notificationMessage = DeliveryNotificationMessage.newBuilder()
                            .setCreationTimestamp(statusDBO.dateCreated!!.toTimestamp())
                            .setEmailMessageId(email.id.emailId)
                            .setMessageQueued(MessageQueued.getDefaultInstance())
                            .build()
                    )
                } else {
                    val statusDBO = EmailStatusEventDBO(
                        email = email,
                        emailStatus = EmailStatus.HARD_BOUNCED,
                        metaData = StatusEventMetaData()
                    )
                    emailStatusEventDAO.save(statusDBO)
                    mqSender.queueDeliveryNotification(
                        accountId = email.transaction.authentication.account.id,
                        notificationMessage = DeliveryNotificationMessage.newBuilder()
                            .setCreationTimestamp(statusDBO.dateCreated!!.toTimestamp())
                            .setEmailMessageId(email.id.emailId)
                            .setMessageHardBounced(MessageHardBounced.getDefaultInstance())
                            .build()
                    )

                    LOGGER.error { "Wasn't able to deliver message with emailId: $emailId" }
                }
            }

            override fun onError(t: Throwable) {
                if ((t as? StatusRuntimeException)?.status != Status.CANCELLED) {
                    // Only log if there's something more interesting than a closed connection
                    LOGGER.error(t) { "Error in MTAEmailQueueService" }
                }
                future.cancel(true)
            }
        }
    }

    fun incomingEmail(request: IncomingEmailMessage): MailAcceptanceResult.AcceptanceStatus {
        val body = request.body.toByteArray()
        val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), body.inputStream())
        val receiverAddress = InternetAddress(request.emailAddress)
        val recipientEmail = Email(receiverAddress.address)
        val accountTypePair = handleUnsubscribe(recipientEmail)
            ?: handleHardBounce(recipientEmail)
            ?: handleNormalEmails(recipientEmail)

        // <> from address means bounce
        if (request.from == "<>") {
            LOGGER.warn { "Received bounce to $recipientEmail" }
            return MailAcceptanceResult.AcceptanceStatus.IGNORED
        } else if (accountTypePair != null) {
            val fromAddress = InternetAddress(request.from)
            val fromEmail = Email(fromAddress.address)
            val accountDBO = accountTypePair.first
            val incomingEmailDBO = IncomingEmailDBO(
                body = body,
                fromEmail = fromEmail,
                fromName = fromAddress.personal ?: "",
                receiverEmail = recipientEmail,
                receiverName = receiverAddress.personal ?: "",
                account = accountDBO,
                subject = mimeMessage.subject
            )
            incomingEmailDAO.save(incomingEmailDBO)
            if (accountTypePair.second == IncomingEmailType.NORMAL) {
                val notificationMessage = IncomingEmailNotificationMessage.newBuilder()
                    .setIncomingEmailMessageId(incomingEmailDBO.id.incomingEmailId)
                    .build()
                mqSender.queueIncomingEmailNotification(accountDBO.id, notificationMessage)
            }
            return MailAcceptanceResult.AcceptanceStatus.ACCEPTED
        } else {
            return MailAcceptanceResult.AcceptanceStatus.REJECTED
        }
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
                        emailStatus = EmailStatus.HARD_BOUNCED,
                        metaData = StatusEventMetaData()
                    )
                    emailStatusEventDAO.save(emailStatusEventDBO)
                    blockedEmailDAO.block(emailStatusEventDBO)
                    val notificationMessage = DeliveryNotificationMessage
                        .newBuilder()
                        .setCreationTimestamp(clock.instant().toTimestamp())
                        .setEmailMessageId(email.id.emailId)
                        .setMessageHardBounced(MessageHardBounced.getDefaultInstance())
                        .build()
                    mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessage)
                    email.transaction.authentication.account to IncomingEmailType.HARD_BOUNCE
                }
        } else {
            null
        }
    }

    private fun handleUnsubscribe(recipientAddress: Email): Pair<AccountDBO, IncomingEmailType>? {
        return if (recipientAddress.address.startsWith("unsub-")) {
            val messageId = MessageId(recipientAddress.address.substringAfter('-'))
            emailDAO.getByMessageId(messageId)
                ?.let { email ->
                    val emailStatusEventDBO = EmailStatusEventDBO(
                        email = email,
                        emailStatus = EmailStatus.UNSUBSCRIBE,
                        metaData = StatusEventMetaData()
                    )
                    emailStatusEventDAO.save(emailStatusEventDBO)
                    blockedEmailDAO.block(emailStatusEventDBO)
                    val notificationMessage = DeliveryNotificationMessage
                        .newBuilder()
                        .setCreationTimestamp(clock.instant().toTimestamp())
                        .setEmailMessageId(email.id.emailId)
                        .setMessageUnsubscribed(MessageUnsubscribed.getDefaultInstance())
                        .build()
                    mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessage)
                    email.transaction.authentication.account to IncomingEmailType.UNSUBSCRIBE
                }
        } else {
            null
        }
    }

    companion object {
        private val LOGGER = logger()
        private val responseCloser = Executors.newCachedThreadPool()
    }
}

enum class IncomingEmailType {
    UNSUBSCRIBE,
    HARD_BOUNCE,
    NORMAL
}
