package com.sourceforgery.tachikoma.identifiers

import java.util.concurrent.atomic.AtomicInteger

class MessageIdFactoryMock : MessageIdFactory {
    private var counter = AtomicInteger()
    override fun createMessageId() =
            MessageId("not-really-random${counter.incrementAndGet()}@example.com")
}