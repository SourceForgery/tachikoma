package com.sourceforgery.tachikoma.database.objects.scalars

import com.sourceforgery.tachikoma.identifiers.AutoMailId
import io.ebean.config.ScalarTypeConverter

@Suppress("unused")
class AutoMailIdScalarType : ScalarTypeConverter<AutoMailId, String> {
    override fun getNullValue(): AutoMailId? {
        return null
    }

    override fun wrapValue(autoMailId: String): AutoMailId {
        return AutoMailId(autoMailId)
    }

    override fun unwrapValue(messageId: AutoMailId): String {
        return messageId.autoMailId
    }
}
