package com.sourceforgery.tachikoma.mta

import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.ExtractEmailMetadata
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
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.IncomingEmailNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.MessageHardBounced
import com.sourceforgery.tachikoma.mq.MessageQueued
import com.sourceforgery.tachikoma.mq.MessageUnsubscribed
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.time.Clock
import java.util.Properties

class MTAEmailQueueService(override val di: DI) : DIAware {
    private val clock: Clock by instance()
    private val mqSequenceFactory: MQSequenceFactory by instance()
    private val emailDAO: EmailDAO by instance()
    private val incomingEmailDAO: IncomingEmailDAO by instance()
    private val emailStatusEventDAO: EmailStatusEventDAO by instance()
    private val blockedEmailDAO: BlockedEmailDAO by instance()
    private val mqSender: MQSender by instance()
    private val incomingEmailAddressDAO: IncomingEmailAddressDAO by instance()
    private val extractEmailMetadata: ExtractEmailMetadata by instance()

    fun getEmails(
        requests: Flow<MTAQueuedNotification>,
        mailDomain: MailDomain,
    ): Flow<EmailMessage> {
        LOGGER.info { "MTA connected with mail domain $mailDomain" }

        val blockingDispatcher = ServiceRequestContext.current().blockingTaskExecutor().asCoroutineDispatcher()
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(blockingDispatcher) {
            requests.collect { value ->
                if (value.equals(MTAQueuedNotification.getDefaultInstance())) {
                    return@collect
                }
                val queueId = value.queueId
                val emailId = EmailId(value.emailId)
                val email = emailDAO.getByEmailId(emailId)
                if (email == null) {
                    LOGGER.warn { "Didn't find id for email with emailId: $emailId" }
                    return@collect
                }

                if (value.success) {
                    check(queueId.isNotBlank()) {
                        "No queueId set in spite of successful delivery of email $emailId"
                    }
                    LOGGER.debug { "Successfully delivered email with id $emailId and mtaQueueId $queueId" }
                    email.mtaQueueId = queueId
                    emailDAO.save(email)

                    val statusDBO =
                        EmailStatusEventDBO(
                            email = email,
                            emailStatus = EmailStatus.QUEUED,
                            metaData = StatusEventMetaData(),
                        )
                    emailStatusEventDAO.save(statusDBO)
                    mqSender.queueDeliveryNotification(
                        accountId = email.transaction.authentication.account.id,
                        notificationMessage =
                            DeliveryNotificationMessage.newBuilder()
                                .setEmailMessageId(email.id.emailId)
                                .setMessageQueued(MessageQueued.getDefaultInstance())
                                .build(),
                    )
                } else {
                    val statusDBO =
                        EmailStatusEventDBO(
                            email = email,
                            emailStatus = EmailStatus.HARD_BOUNCED,
                            metaData = StatusEventMetaData(),
                        )
                    emailStatusEventDAO.save(statusDBO)
                    mqSender.queueDeliveryNotification(
                        accountId = email.transaction.authentication.account.id,
                        notificationMessage =
                            DeliveryNotificationMessage.newBuilder()
                                .setEmailMessageId(email.id.emailId)
                                .setMessageHardBounced(MessageHardBounced.getDefaultInstance())
                                .build(),
                    )

                    LOGGER.error { "Wasn't able to deliver message with emailId: $emailId" }
                }
            }
        }

        return mqSequenceFactory.listenForOutgoingEmails(mailDomain)
            .mapNotNull {
                val email = emailDAO.fetchEmailData(EmailId(it.emailId))
                if (email == null) {
                    LOGGER.warn { "Nothing found when looking trying to send email with id: " + it.emailId }
                    null
                } else {
                    LOGGER.info { "Email with id ${email.id} is about to be sent" }
                    EmailMessage.newBuilder()
                        .setBody(email.body!!)
                        .setFrom(email.transaction.fromEmail.address)
                        .setEmailId(email.id.emailId)
                        .setEmailAddress(email.recipient.address)
                        .addAllBcc(email.transaction.bcc)
                        .build()
                }
            }
    }

    fun incomingEmail(request: IncomingEmailMessage): MailAcceptanceResult.AcceptanceStatus {
        val body = request.body.toByteArray()
        val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), body.inputStream())
        val receiverAddress = InternetAddress(request.emailAddress)
        val recipientEmail = Email(receiverAddress.address)
        val accountTypePair =
            handleUnsubscribe(recipientEmail)
                ?: handleHardBounce(recipientEmail)
                ?: handleNormalEmails(recipientEmail)

        // "" from address means bounce
        if (request.from == "") {
            LOGGER.warn { "Received bounce to $recipientEmail" }
            return MailAcceptanceResult.AcceptanceStatus.IGNORED
        } else if (accountTypePair != null) {
            val accountDBO = accountTypePair.first

            val emails = extractEmailMetadata.extract(body)
            val incomingEmailDBO =
                IncomingEmailDBO(
                    body = body,
                    mailFrom = Email(request.from),
                    recipient = Email(request.emailAddress),
                    fromEmails = emails.from,
                    toEmails = emails.to,
                    replyToEmails = emails.replyTo,
                    account = accountDBO,
                    subject = mimeMessage.subject ?: "",
                )
            incomingEmailDAO.save(incomingEmailDBO)
            if (accountTypePair.second == IncomingEmailType.NORMAL) {
                val notificationMessage =
                    IncomingEmailNotificationMessage.newBuilder()
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
            val autoMailId = AutoMailId(recipientAddress.address.substringAfter('-'))
            emailDAO.getByAutoMailId(autoMailId)
                ?.let { email ->
                    val emailStatusEventDBO =
                        EmailStatusEventDBO(
                            email = email,
                            emailStatus = EmailStatus.HARD_BOUNCED,
                            metaData = StatusEventMetaData(),
                        )
                    emailStatusEventDAO.save(emailStatusEventDBO)
                    blockedEmailDAO.block(emailStatusEventDBO)
                    val notificationMessage =
                        DeliveryNotificationMessage
                            .newBuilder()
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
            val autoMailId = AutoMailId(recipientAddress.address.substringAfter('-'))
            emailDAO.getByAutoMailId(autoMailId)
                ?.let { email ->
                    val emailStatusEventDBO =
                        EmailStatusEventDBO(
                            email = email,
                            emailStatus = EmailStatus.UNSUBSCRIBE,
                            metaData = StatusEventMetaData(),
                        )
                    emailStatusEventDAO.save(emailStatusEventDBO)
                    blockedEmailDAO.block(emailStatusEventDBO)
                    val notificationMessage =
                        DeliveryNotificationMessage
                            .newBuilder()
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
    }
}

enum class IncomingEmailType {
    UNSUBSCRIBE,
    HARD_BOUNCE,
    NORMAL,
}
