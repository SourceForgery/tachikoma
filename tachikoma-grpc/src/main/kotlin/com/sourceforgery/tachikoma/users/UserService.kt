package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.auth.InternalCreateUserService
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ApiToken
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUser
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserResponse
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveUserResponse
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toFrontendRole
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toUserId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class UserService(
    override val di: DI
) : DIAware {
    private val accountDAO: AccountDAO by instance()
    private val authenticationDAO: AuthenticationDAO by instance()
    private val internalCreateUsers: InternalCreateUserService by instance()

    fun addFrontendUser(request: AddUserRequest): ModifyUserResponse {
        val role = frontendRole(request.authenticationRole)
        val password: String? = request.passwordAuth.password.emptyToNull()
        val login: String? = request.passwordAuth.login.emptyToNull()
        val recipientOverride = if (request.hasRecipientOverride() && request.recipientOverride.email.isNotEmpty()) {
            request.recipientOverride.toEmail()
        } else {
            null
        }
        val account = accountDAO.get(MailDomain(request.mailDomain))!!

        val newAuth = internalCreateUsers.createFrontendAuthentication(
            role = role,
            addApiToken = request.addApiToken,
            login = login,
            password = password,
            active = request.active,
            account = account,
            recipientOverride = recipientOverride
        )
        return ModifyUserResponse.newBuilder()
            .apply {
                user = toUser(newAuth)
                apiToken = newAuth.apiToken.orEmpty()
            }
            .build()
    }

    private fun frontendRole(userRole: FrontendUserRole) =
        when (userRole) {
            FrontendUserRole.FRONTEND -> AuthenticationRole.FRONTEND
            FrontendUserRole.FRONTEND_ADMIN -> AuthenticationRole.FRONTEND_ADMIN
            else -> throw IllegalArgumentException("$userRole is not implemented")
        }

    fun modifyFrontendUser(request: ModifyUserRequest, auth: AuthenticationDBO): ModifyUserResponse {
        val addApiToken = request.apiToken == ApiToken.RESET_API_TOKEN
        if (request.apiToken == ApiToken.REMOVE_API_TOKEN) {
            auth.apiToken = null
        }
        if (request.hasRecipientOverride()) {
            auth.recipientOverride = request.recipientOverride.email.takeUnless { it.isBlank() }
                ?.let { Email(it) }
        }
        auth.role = frontendRole(request.authenticationRole)
        auth.active = request.active
        if (addApiToken) {
            internalCreateUsers.setApiToken(auth)
        }

        if (request.hasNewPassword()) {
            if (auth.login != null) {
                auth.encryptedPassword = request.newPassword.takeUnless { it.isBlank() }
                    ?.let { PasswordStorage.createHash(it) }
            } else {
                throw IllegalArgumentException("Trying to set password when there's no login")
            }
        }

        authenticationDAO.save(auth)
        return ModifyUserResponse.newBuilder()
            .apply {
                user = toUser(auth)
                if (addApiToken) {
                    apiToken = auth.apiToken!!
                }
            }
            .build()
    }

    fun getFrontendUsers(mailDomain: MailDomain): Flow<FrontendUser> =
        accountDAO.get(mailDomain = mailDomain)!!
            .authentications
            .asFlow()
            .filter { it.role == AuthenticationRole.FRONTEND_ADMIN || it.role == AuthenticationRole.FRONTEND }
            .map { toUser(it) }

    fun removeUser(request: RemoveUserRequest): RemoveUserResponse {
        authenticationDAO.deleteById(request.userToRemove.toAuthenticationId())
        return RemoveUserResponse.getDefaultInstance()
    }

    private fun toUser(auth: AuthenticationDBO): FrontendUser {
        return FrontendUser.newBuilder()
            .apply {
                active = auth.active
                authId = auth.id.toUserId()
                authenticationRole = auth.role.toFrontendRole()
                dateCreated = auth.dateCreated!!.toTimestamp()
                lastUpdated = auth.lastUpdated!!.toTimestamp()
                hasPassword = auth.encryptedPassword != null
                login = auth.login.orEmpty()
                mailDomain = auth.account.mailDomain.mailDomain
                auth.recipientOverride?.also {
                    recipientOverride = it.toGrpcInternal()
                }
                hasApiToken = auth.apiToken != null
            }
            .build()
    }
}
