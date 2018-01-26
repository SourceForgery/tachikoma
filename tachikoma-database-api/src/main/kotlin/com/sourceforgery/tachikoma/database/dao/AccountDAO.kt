package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId

interface AccountDAO {
    fun save(account: AccountDBO)
    fun getById(accountId: AccountId): AccountDBO
}