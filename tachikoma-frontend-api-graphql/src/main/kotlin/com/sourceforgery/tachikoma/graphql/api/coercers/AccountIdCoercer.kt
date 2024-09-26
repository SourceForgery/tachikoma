package com.sourceforgery.tachikoma.graphql.api.coercers

import com.sourceforgery.tachikoma.identifiers.AccountId
import java.math.BigInteger

object AccountIdCoercer : AbstractIntCoercer<AccountId>() {
    override val clazz = AccountId::class
    override val description: String =
        """
        A type representing a MailDomain
        """.trimIndent()

    override fun fromInt(input: BigInteger): AccountId = AccountId(input.longValueExact())

    override fun toInt(input: AccountId?): BigInteger? = input?.accountId?.toBigInteger()
}
