package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface MQSender {
    fun queueJob(jobMessage: JobMessage)

    fun queueOutgoingEmail(
        mailDomain: MailDomain,
        outgoingEmailMessage: OutgoingEmailMessage,
    )

    fun queueDeliveryNotification(
        accountId: AccountId,
        notificationMessage: DeliveryNotificationMessage,
    )

    fun queueIncomingEmailNotification(
        accountId: AccountId,
        incomingEmailNotificationMessage: IncomingEmailNotificationMessage,
    )

    fun queueEmailBlockingNotification(
        accountId: AccountId,
        emailBlockedMessage: BlockedEmailAddressEvent,
    )
}
