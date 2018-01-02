package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.AccountId

interface MQSender {
    fun queueJob(jobMessage: JobMessage)
    fun queueNotification(accountId: AccountId, notificationMessage: NotificationMessage)
}