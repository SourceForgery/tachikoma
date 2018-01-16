package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId

interface AccountDAO {
    fun getById(accountId: AccountId): AccountDBO
}