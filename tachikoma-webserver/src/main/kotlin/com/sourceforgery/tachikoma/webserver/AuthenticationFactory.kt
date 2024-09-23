package com.sourceforgery.tachikoma.webserver

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.hash.Hashing
import com.google.common.hash.Hashing.hmacSha1
import com.linecorp.armeria.common.HttpHeaders
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.exceptions.NoAuthorizationCredentialsException
import com.sourceforgery.tachikoma.grpc.frontend.auth.AuthRole
import com.sourceforgery.tachikoma.grpc.frontend.auth.WebTokenAuthData
import com.sourceforgery.tachikoma.grpc.frontend.toAccountId
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.netty.util.AsciiString
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider
import java.util.Base64
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class AuthenticationFactory(override val di: DI) : DIAware {
    private val httpHeaders by provider<HttpHeaders>()
    private val webServerConfig: WebServerConfig by instance()
    private val authenticationDAO: AuthenticationDAO by instance()
    private val accountDAO: AccountDAO by instance()

    private val apiKeyCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(CacheLoader.from<String, Authentication?> { parseApiTokenHeader(it) })

    @Suppress("UnstableApiUsage")
    private val authHmac by lazy {
        hmacSha1(webServerConfig.webtokenSignKey)
    }

    fun provide() =
        parseWebTokenHeader()
            ?: parseApiTokenHeader()
            ?: NO_AUTHENTICATION

    private fun parseApiTokenHeader(): Authentication? =
        httpHeaders()[APITOKEN_HEADER]
            ?.let {
                try {
                    apiKeyCache[it]
                } catch (e: ExecutionException) {
                    throw e.cause!!
                }
            }

    private fun parseApiTokenHeader(header: String?): Authentication =
        header
            ?.let {
                MailDomain(it.substringBefore(':')) to it.substringAfter(':')
            }
            ?.let { splitAuthString ->
                authenticationDAO.validateApiToken(splitAuthString.second)
                    ?.let { auth ->
                        if (auth.account.mailDomain == splitAuthString.first) {
                            AuthenticationImpl(
                                // No webtoken should allow backend
                                role = auth.role,
                                authenticationId = auth.id,
                                accountId = auth.account.id,
                                accountDAO = accountDAO
                            )
                        } else {
                            LOGGER.warn { "Incorrect domain(${splitAuthString.first}) for account ${auth.account.id})" }
                            wrapException("Incorrect domain(${splitAuthString.first}")
                        }
                    }
                    ?: wrapException("No api key found when trying to auth domain (${splitAuthString.first})")
            }
            ?: NO_AUTHENTICATION

    private fun wrapException(error: String): Authentication {
        return ThrowingAuthentication { InvalidOrInsufficientCredentialsException(error) }
    }

    private fun parseWebTokenHeader(): Authentication? {
        val webtokenHeader = httpHeaders()[WEBTOKEN_HEADER]
            ?: return null
        val splitToken = webtokenHeader.split(
            limit = 2,
            delimiters = charArrayOf('.')
        )
        if (splitToken.size != 2) {
            return null
        }
        val payloadSignature = BASE64_DECODER.decode(splitToken[0])!!
        val payload = BASE64_DECODER.decode(splitToken[1])!!
        if (authHmac.hashBytes(payload).asBytes().contentEquals(payloadSignature)) {
            return null
        }
        val tokenAuthData = WebTokenAuthData.parseFrom(payload)
        val authenticationId = tokenAuthData.toAuthenticationId()
            ?: throw InvalidOrInsufficientCredentialsException()

        val accountId = tokenAuthData.toAccountId()
            ?: throw InvalidOrInsufficientCredentialsException()
        return AuthenticationImpl(
            // No webtoken should allow backend
            role = tokenAuthData.authenticationRole.toAuthenticationRole(),
            authenticationId = authenticationId,
            accountId = accountId,
            accountDAO = accountDAO
        )
    }

    companion object {
        val LOGGER = logger()
        val BASE64_DECODER = Base64.getDecoder()!!
        val NO_AUTHENTICATION: Authentication =
            ThrowingAuthentication { NoAuthorizationCredentialsException("No auth credentials") }

        val WEBTOKEN_HEADER = AsciiString("x-webtoken")
        val APITOKEN_HEADER = AsciiString("x-apitoken")
    }
}

internal class AuthenticationImpl(
    override var authenticationId: AuthenticationId,
    override var accountId: AccountId,
    private val accountDAO: AccountDAO,
    private val role: AuthenticationRole
) : Authentication {

    override val mailDomain: MailDomain by lazy {
        accountDAO.get(accountId).mailDomain
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
        if (!isAdmin() && this.mailDomain != mailDomain) {
            throw InvalidOrInsufficientCredentialsException("auth domain (${this.mailDomain}) is not the same as the request one ($mailDomain")
        }
        if (role != AuthenticationRole.FRONTEND_ADMIN) {
            throw InvalidOrInsufficientCredentialsException("Invalid or insufficient credentials")
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
        TODO("No superadmin yet")
    }

    private fun isAdmin() = false

    private fun requireValid() {
        if (!valid) {
            throw InvalidOrInsufficientCredentialsException()
        }
    }

    override val valid: Boolean = true
}

fun AuthRole.toAuthenticationRole() =
    when (this) {
        AuthRole.FRONTEND -> AuthenticationRole.FRONTEND
        AuthRole.BACKEND -> AuthenticationRole.BACKEND
        AuthRole.FRONTEND_ADMIN -> AuthenticationRole.FRONTEND_ADMIN
        AuthRole.UNRECOGNIZED -> throw InvalidOrInsufficientCredentialsException("Webtoken is invalid")
    }

private class ThrowingAuthentication(private val t: () -> RuntimeException) : Authentication {
    override fun requireFrontend(): AccountId = throw t()

    override fun requireBackend(): AccountId = throw t()

    override fun requireFrontendAdmin(mailDomain: MailDomain): AccountId = throw t()

    override fun requireAdmin(): AccountId = throw t()

    override val mailDomain: MailDomain
        get() = throw t()

    override val authenticationId: AuthenticationId
        get() = throw t()

    override val accountId: AccountId
        get() = throw t()

    override val valid: Boolean = false
}
