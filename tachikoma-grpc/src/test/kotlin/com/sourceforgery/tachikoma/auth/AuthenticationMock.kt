package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain

class AuthenticationMock(
) : Authentication {
    override var valid: Boolean = false
    override val authenticationId: AuthenticationId
        get() = _authenticationId!!
    override val accountId: AccountId
        get() = _accountId!!
    override val mailDomain: MailDomain
        get() = _mailDomain!!

    private var _authenticationId: AuthenticationId? = null
    private var _accountId: AccountId? = null
    private var _mailDomain: MailDomain? = null

    private var role: AuthenticationRole? = null

    fun from(authenticationDBO: AuthenticationDBO) {
        role = authenticationDBO.role
        valid = true
        _authenticationId = authenticationDBO.id
        _accountId = authenticationDBO.account.id
        _mailDomain = authenticationDBO.account.mailDomain
    }

    fun from(
            role: AuthenticationRole,
            mailDomain: MailDomain
    ) {
        this.role = role
        this._mailDomain = mailDomain
        this._accountId = null
        this._authenticationId = null
        this.valid = true
    }

    fun invalidAuth() {
        this.role = null
        this._mailDomain = null
        this._accountId = null
        this._authenticationId = null
        this.valid = false
    }

    override fun requireFrontend(): AccountId {
        requireValid()
        if (role != AuthenticationRole.FRONTEND_ADMIN && role != AuthenticationRole.FRONTEND) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    override fun requireFrontendAdmin(mailDomain: MailDomain): AccountId {
        requireValid()
        if (role != AuthenticationRole.FRONTEND_ADMIN) {
            throw InvalidOrInsufficientCredentialsException()
        }
        if (_mailDomain != mailDomain) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    override fun requireBackend(): AccountId {
        requireValid()
        if (role != AuthenticationRole.BACKEND) {
            throw InvalidOrInsufficientCredentialsException()
        }
        return accountId
    }

    override fun requireAdmin(): AccountId {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun requireValid() {
        if (!valid) {
            throw InvalidOrInsufficientCredentialsException()
        }
    }
}