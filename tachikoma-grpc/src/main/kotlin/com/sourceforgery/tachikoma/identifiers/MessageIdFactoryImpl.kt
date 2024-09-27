package com.sourceforgery.tachikoma.identifiers

import org.kodein.di.DI
import org.kodein.di.DIAware
import java.util.UUID

class MessageIdFactoryImpl(override val di: DI) : MessageIdFactory, DIAware {
    override fun createMessageId(domain: MailDomain) = MessageId("${UUID.randomUUID()}@$domain")
}
