package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO

interface IncomingEmailAddressDAO {
    fun save(incomingEmailAddressDBO: IncomingEmailAddressDBO)

    fun getByEmail(email: Email): IncomingEmailAddressDBO?

    fun getAll(accountDBO: AccountDBO): List<IncomingEmailAddressDBO>

    fun delete(
        accountDBO: AccountDBO,
        localPart: String,
    ): Int
}
