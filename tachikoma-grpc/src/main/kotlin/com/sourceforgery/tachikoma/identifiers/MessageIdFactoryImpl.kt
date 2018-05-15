package com.sourceforgery.tachikoma.identifiers

import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.util.UUID
import javax.inject.Inject

class MessageIdFactoryImpl
@Inject
private constructor(
    private val trackingConfig: TrackingConfig
) : MessageIdFactory {
    override fun createMessageId() =
        MessageId("${UUID.randomUUID()}@${trackingConfig.baseUrl.host}")
}