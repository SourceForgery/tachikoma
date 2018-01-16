package com.sourceforgery.tachikoma.database.objects.scalars

import com.sourceforgery.tachikoma.identifiers.MessageId
import io.ebean.config.ScalarTypeConverter

@Suppress("unused")
class MessageIdScalarType : ScalarTypeConverter<MessageId, String> {
    override fun getNullValue(): MessageId? {
        return null
    }

    override fun wrapValue(messageId: String): MessageId {
        return MessageId(messageId)
    }

    override fun unwrapValue(messageId: MessageId): String {
        return messageId.messageId
    }
}