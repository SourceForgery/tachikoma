package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.UserId

interface Authentication {
    val valid: Boolean
    val userId: UserId?
    val allowBackend: Boolean
    val accountId: AccountId?
}