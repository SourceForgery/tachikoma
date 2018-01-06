package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpHeaders
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.common.HmacUtil.validateHmacSha1
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.auth.WebTokenAuthData
import com.sourceforgery.tachikoma.grpc.frontend.toAccountId
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.netty.util.AsciiString
import org.glassfish.hk2.api.Factory
import java.util.Base64
import javax.inject.Inject

class AuthenticationFactory
@Inject
private constructor(
        private val httpHeaders: HttpHeaders,
        private val webServerConfig: WebServerConfig,
        private val authenticationDAO: AuthenticationDAO
) : Factory<Authentication> {
    override fun provide() =
            parseWebTokenHeader()
                    ?: parseApiTokenHeader()
                    ?: NO_AUTHENTICATION

    private fun parseApiTokenHeader() =
            httpHeaders[APITOKEN_HEADER]
            ?.let {
                authenticationDAO.validateApiToken(it)
            }
            ?.let {
                AuthenticationImpl(
                        // No webtoken should allow backend
                        allowBackend = false,
                        authenticationId = it.id,
                        accountId = it.account?.id
                )
            }

    private fun parseWebTokenHeader(): Authentication? {
        val webtokenHeader = httpHeaders[WEBTOKEN_HEADER]
                ?: return null
        val splitToken = webtokenHeader.split(
                delimiters = *charArrayOf('.'),
                limit = 2
        )
        if (splitToken.size != 2) {
            return null
        }
        val payloadSignature = splitToken[0]
        val payload = BASE64_DECODER.decode(splitToken[1])
        if (!validateHmacSha1(webServerConfig.webtokenSignKey, payload, payloadSignature)) {
            return null
        }
        val tokenAuthData = WebTokenAuthData.parseFrom(payload)
        return AuthenticationImpl(
                // No webtoken should allow backend
                allowBackend = false,
                authenticationId = tokenAuthData.toAuthenticationId(),
                accountId = tokenAuthData.toAccountId()
        )
    }

    override fun dispose(instance: Authentication) {
    }

    companion object {
        val BASE64_DECODER = Base64.getDecoder()!!
        val NO_AUTHENTICATION = object : Authentication {
            override val authenticationId: AuthenticationId? = null
            override val accountId: AccountId? = null
            override val allowBackend: Boolean = false
            override val valid: Boolean = false
        }

        val WEBTOKEN_HEADER = AsciiString("x-webtoken")
        val APITOKEN_HEADER = AsciiString("x-apitoken")
    }
}

internal class AuthenticationImpl(
        override var allowBackend: Boolean = false,
        override var authenticationId: AuthenticationId? = null,
        override var accountId: AccountId? = null
) : Authentication {
    override val valid: Boolean = true
}