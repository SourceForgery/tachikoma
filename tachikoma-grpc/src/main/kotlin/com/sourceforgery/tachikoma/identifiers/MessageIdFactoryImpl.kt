package com.sourceforgery.tachikoma.identifiers

import java.util.UUID
import javax.inject.Inject

class MessageIdFactoryImpl
@Inject
private constructor() : MessageIdFactory {
    override fun createMessageId(domain: MailDomain) =
        MessageId("${UUID.randomUUID()}@$domain")
}