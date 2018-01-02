package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.UserId
import java.io.Closeable

interface MQSequenceFactory {
    fun listen(userId: UserId, callback: (NotificationMessage) -> Unit): Closeable
    fun listenForJobs(callback: (JobMessage) -> Unit): Closeable
}
