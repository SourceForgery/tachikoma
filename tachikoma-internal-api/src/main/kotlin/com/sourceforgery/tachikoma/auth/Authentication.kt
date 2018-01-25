package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface Authentication {
    val valid: Boolean
    val authenticationId: AuthenticationId
    val accountId: AccountId
    fun requireFrontend(): AccountId
    fun requireFrontendAdmin(): AccountId
    fun requireBackend(): AccountId
    val mailDomain: MailDomain
}