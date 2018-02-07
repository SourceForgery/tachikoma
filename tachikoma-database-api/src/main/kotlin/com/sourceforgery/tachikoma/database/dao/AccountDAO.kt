package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface AccountDAO {
    fun save(account: AccountDBO)
    fun getById(accountId: AccountId): AccountDBO
    fun getByMailDomain(mailDomain: MailDomain): AccountDBO?
}