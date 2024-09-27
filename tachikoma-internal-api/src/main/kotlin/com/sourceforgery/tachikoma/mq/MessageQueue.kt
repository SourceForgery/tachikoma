package com.sourceforgery.tachikoma.mq

import java.time.Duration

interface MessageQueue<T> {
    val delay: Duration
    val maxLength: Int?
    val nextDestination: MessageQueue<T>?
    val name: String
    val parser: (ByteArray) -> T
}
