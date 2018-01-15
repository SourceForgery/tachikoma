package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AccountId

interface MQSender {
    fun queueJob(jobMessage: JobMessage)
    fun queueOutgoingEmail(outgoingEmailMessage: OutgoingEmailMessage)
    fun queueNotification(accountId: AccountId, notificationMessage: DeliveryNotificationMessage)
    fun queueIncomingEmailNotification(accountId: AccountId, incomingEmailNotificationMessage: IncomingEmailNotificationMessage)
}