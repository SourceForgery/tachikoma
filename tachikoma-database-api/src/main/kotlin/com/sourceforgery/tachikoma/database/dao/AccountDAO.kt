package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface AccountDAO {
    fun save(account: AccountDBO)

    operator fun get(accountId: AccountId): AccountDBO

    operator fun get(mailDomain: MailDomain): AccountDBO?
}
