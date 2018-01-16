package com.sourceforgery.tachikoma.database.objects.scalars

import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.ebean.config.ScalarTypeConverter

@Suppress("unused")
class MailDomainScalarType : ScalarTypeConverter<MailDomain, String> {
    override fun getNullValue(): MailDomain? {
        return null
    }

    override fun wrapValue(mailDomain: String): MailDomain {
        return MailDomain(mailDomain)
    }

    override fun unwrapValue(mailDomain: MailDomain): String {
        return mailDomain.mailDomain
    }
}