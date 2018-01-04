package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.UserId
import java.io.Closeable

interface MQSequenceFactory {
    fun listenForDeliveryNotifications(userId: UserId, callback: (DeliveryNotificationMessage) -> Unit): Closeable
    fun listenForJobs(callback: (JobMessage) -> Unit): Closeable
}
