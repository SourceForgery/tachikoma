package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface Authentication {
    val valid: Boolean
    val authenticationId: AuthenticationId
    val accountId: AccountId
    fun requireFrontend(): AccountId
    fun requireFrontendAdmin(mailDomain: MailDomain): AccountId
    fun requireBackend(): AccountId
    fun requireAdmin(): AccountId
    val mailDomain: MailDomain
}