package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.timestamp
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.time.Clock
import java.util.concurrent.LinkedBlockingQueue

class MQSenderMock(override val di: DI) : MQSender, DIAware {
    val deliveryNotifications = LinkedBlockingQueue<EmailNotificationEvent>()
    val jobs = LinkedBlockingQueue<JobMessage>()
    val outgoingEmails = LinkedBlockingQueue<OutgoingEmailMessage>()
    val incomingEmails = LinkedBlockingQueue<IncomingEmailNotificationMessage>()
    val clock: Clock by instance()

    override fun queueJob(jobMessage: JobMessage) {
        jobs.add(jobMessage)
    }

    override fun queueOutgoingEmail(
        mailDomain: MailDomain,
        outgoingEmailMessage: OutgoingEmailMessage,
    ) {
        outgoingEmails.add(outgoingEmailMessage)
    }

    override fun queueDeliveryNotification(
        accountId: AccountId,
        notificationMessage: DeliveryNotificationMessage,
    ) {
        deliveryNotifications.add(
            EmailNotificationEvent.newBuilder()
                .setCreationTimestamp(clock.timestamp())
                .setDeliveryNotification(notificationMessage)
                .build(),
        )
    }

    override fun queueIncomingEmailNotification(
        accountId: AccountId,
        incomingEmailNotificationMessage: IncomingEmailNotificationMessage,
    ) {
        incomingEmails.add(incomingEmailNotificationMessage)
    }

    override fun queueEmailBlockingNotification(
        accountId: AccountId,
        emailBlockedMessage: BlockedEmailAddressEvent,
    ) {
        deliveryNotifications.add(
            EmailNotificationEvent.newBuilder()
                .setCreationTimestamp(clock.timestamp())
                .setBlockedEmailAddress(emailBlockedMessage)
                .build(),
        )
    }
}
