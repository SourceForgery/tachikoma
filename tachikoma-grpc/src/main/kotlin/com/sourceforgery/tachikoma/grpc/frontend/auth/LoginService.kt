package com.sourceforgery.tachikoma.grpc.frontend.auth

import com.sourceforgery.tachikoma.common.HmacUtil
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.config.WebtokenAuthConfig
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toRole
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.util.Base64
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class LoginService(override val di: DI) : DIAware {
    private val authenticationDAO: AuthenticationDAO by instance()
    private val webtokenAuthConfig: WebtokenAuthConfig by instance()

    fun login(loginRequest: LoginRequest): LoginResponse {
        val auth = authenticationDAO.validateApiToken(loginRequest.username)
        val correct = auth
            ?.encryptedPassword
            ?.let {
                PasswordStorage.verifyPassword(
                    password = loginRequest.password,
                    correctHash = it
                )
            }
        if (correct != true) {
            throw throw StatusRuntimeException(Status.PERMISSION_DENIED)
        }
        return LoginResponse.newBuilder()
            .setAuthHeader(createWebtoken(auth))
            .build()
    }

    private fun createWebtoken(auth: AuthenticationDBO): String =
        WebTokenAuthData.newBuilder()
            .setAccountId(auth.account.id.accountId)
            .setAuthenticationRole(auth.role.toRole())
            .setUserId(auth.id.authenticationId)
            .build()
            .let {
                val byteArray = it.toByteArray()
                val data = BASE64_ENCODER.encodeToString(byteArray)!!
                val signature = BASE64_ENCODER.encodeToString(HmacUtil.hmacSha1(byteArray, webtokenAuthConfig.webtokenSignKey))!!
                "$signature.$data"
            }

    companion object {
        private val BASE64_ENCODER = Base64.getEncoder()!!
    }
}
