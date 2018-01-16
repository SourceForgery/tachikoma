package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId

interface Authentication {
    val valid: Boolean
    val authenticationId: AuthenticationId
    val allowBackend: Boolean
    val accountId: AccountId?
    fun requireAccount(): AccountId
    fun requireBackend(): AccountId
}