package com.sourceforgery.tachikoma.identifiers

interface MessageIdFactory {
    fun createMessageId(domain: MailDomain): MessageId
}
