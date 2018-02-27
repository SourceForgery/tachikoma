package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.database.auth.InternalCreateUserService
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MQManager
import net.bytebuddy.utility.RandomString
import javax.inject.Inject

class InternalCreateUserServiceImpl
@Inject
private constructor(
        private val mqManager: MQManager,
        private val authenticationDAO: AuthenticationDAO,
        private val accountDAO: AccountDAO
) : InternalCreateUserService {

    private val randomString = RandomString(40)

    override fun createAccount(mailDomain: MailDomain): AccountDBO {
        val account = AccountDBO(mailDomain)
        accountDAO.save(account)
        mqManager.setupAccount(mailDomain)
        return account
    }

    override fun createFrontendAuthentication(
            account: AccountDBO,
            login: String?,
            password: String?,
            role: AuthenticationRole,
            addApiToken: Boolean,
            active: Boolean,
            recipientOverride: Email?
    ): AuthenticationDBO {
        if (role != AuthenticationRole.FRONTEND_ADMIN && role != AuthenticationRole.FRONTEND) {
            throw IllegalArgumentException("Only frontend supported")
        }

        val hasPassword = password.emptyToNull() != null
        val hasLogin = login.emptyToNull() != null
        if (hasLogin != hasPassword) {
            throw IllegalArgumentException("Either both or neither of login & password")
        }

        if (login != null) {
            if (authenticationDAO.getByUsername(login) != null) {
                throw IllegalArgumentException("Username already exists")
            }
        }

        val encryptedPassword = password
                ?.let {
                    PasswordStorage.createHash(it)
                }

        val frontendAuthentication = AuthenticationDBO(
                account = account,
                role = role,
                login = login,
                encryptedPassword = encryptedPassword
        )
        if (addApiToken) {
            setApiToken(frontendAuthentication)
        }

        authenticationDAO.save(frontendAuthentication)
        mqManager.setupAuthentication(
                mailDomain = account.mailDomain,
                authenticationId = frontendAuthentication.id,
                accountId = account.id
        )
        return frontendAuthentication
    }

    override fun setApiToken(authenticationDBO: AuthenticationDBO) {
        authenticationDBO.apiToken = randomString.nextString()
    }

    override fun createBackendAuthentication(account: AccountDBO): AuthenticationDBO {
        val backendAuthentication = AuthenticationDBO(
                apiToken = randomString.nextString(),
                role = AuthenticationRole.BACKEND,
                account = account
        )
        authenticationDAO.save(backendAuthentication)
        return backendAuthentication
    }
}
