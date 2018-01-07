package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import java.io.Closeable

interface MQSequenceFactory {
    fun listenForDeliveryNotifications(authenticationId: AuthenticationId, callback: (DeliveryNotificationMessage) -> Unit): Closeable
    fun listenForJobs(callback: (JobMessage) -> Unit): Closeable
}
