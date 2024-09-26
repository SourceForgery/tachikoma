package com.sourceforgery.tachikoma.graphql.api.coercers

import com.sourceforgery.tachikoma.identifiers.MailDomain

object MailDomainCoercer : AbstractStringCoercer<MailDomain>() {
    override val clazz = MailDomain::class
    override val description: String =
        """
        A type representing a MailDomain
        """.trimIndent()

    override fun fromString(input: String): MailDomain = MailDomain(input)

    override fun toString(input: MailDomain?): String? = input?.mailDomain
}
