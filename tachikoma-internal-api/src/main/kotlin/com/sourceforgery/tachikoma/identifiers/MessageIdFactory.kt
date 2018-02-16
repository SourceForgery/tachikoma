package com.sourceforgery.tachikoma.identifiers

interface MessageIdFactory {
    fun createMessageId(): MessageId
}