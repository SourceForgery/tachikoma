package com.sourceforgery.tachikoma.database.auth

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface InternalCreateUserService {
    fun createAccount(mailDomain: MailDomain): AccountDBO
    fun createBackendAuthentication(account: AccountDBO): AuthenticationDBO
    fun createFrontendAuthentication(
            account: AccountDBO,
            login: String? = null,
            password: String? = null,
            role: AuthenticationRole,
            addApiToken: Boolean,
            active: Boolean = true,
            recipientOverride: Email? = null
    ): AuthenticationDBO

    fun setApiToken(authenticationDBO: AuthenticationDBO)
}
