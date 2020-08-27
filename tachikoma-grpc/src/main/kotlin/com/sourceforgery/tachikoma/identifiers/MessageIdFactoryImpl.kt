package com.sourceforgery.tachikoma.identifiers

import java.util.UUID
import org.kodein.di.DI
import org.kodein.di.DIAware

class MessageIdFactoryImpl(override val di: DI) : MessageIdFactory, DIAware {
    override fun createMessageId(domain: MailDomain) =
        MessageId("${UUID.randomUUID()}@$domain")
}
