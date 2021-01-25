package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import org.kodein.di.DI
import org.kodein.di.DIAware
import java.util.concurrent.LinkedBlockingQueue

class MQSenderMock(override val di: DI) : MQSender, DIAware {
    val deliveryNotifications = LinkedBlockingQueue<DeliveryNotificationMessage>()
    val jobs = LinkedBlockingQueue<JobMessage>()
    val outgoingEmails = LinkedBlockingQueue<OutgoingEmailMessage>()
    val incomingEmails = LinkedBlockingQueue<IncomingEmailNotificationMessage>()

    override fun queueJob(jobMessage: JobMessage) {
        jobs.add(jobMessage)
    }

    override fun queueOutgoingEmail(mailDomain: MailDomain, outgoingEmailMessage: OutgoingEmailMessage) {
        outgoingEmails.add(outgoingEmailMessage)
    }

    override fun queueDeliveryNotification(accountId: AccountId, notificationMessage: DeliveryNotificationMessage) {
        deliveryNotifications.add(notificationMessage)
    }

    override fun queueIncomingEmailNotification(accountId: AccountId, incomingEmailNotificationMessage: IncomingEmailNotificationMessage) {
        incomingEmails.add(incomingEmailNotificationMessage)
    }
}
