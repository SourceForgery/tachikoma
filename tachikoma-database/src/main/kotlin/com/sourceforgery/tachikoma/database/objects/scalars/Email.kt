package com.sourceforgery.tachikoma.database.objects.scalars

import com.sourceforgery.tachikoma.common.Email
import io.ebean.config.ScalarTypeConverter

@Suppress("unused")
class SplitEmailScalarType : ScalarTypeConverter<Email, String> {
    override fun getNullValue(): Email? {
        return null
    }

    override fun wrapValue(email: String): Email {
        return Email(email)
    }

    override fun unwrapValue(email: Email): String {
        return email.address
    }
}