package com.sourceforgery.tachikoma.identifiers

import java.util.UUID

class MessageIdFactoryImpl : MessageIdFactory {
    override fun createMessageId(mailDomain: MailDomain) =
            MessageId("${UUID.randomUUID()}@$mailDomain")
}