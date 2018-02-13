package com.sourceforgery.tachikoma.identifiers

interface MessageIdFactory {
    fun createMessageId(mailDomain: MailDomain): MessageId
}