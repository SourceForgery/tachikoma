package com.sourceforgery.tachikoma.rest.unsubscribe

import com.fasterxml.jackson.databind.ObjectMapper
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.Server
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.MinimalBinder
import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.grpc.frontend.toFrontendRole
import com.sourceforgery.tachikoma.hk2.getValue
import com.sourceforgery.tachikoma.hk2.hk2
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.rest.RestBinder
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.users.UserService
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import io.ebean.EbeanServer
import java.net.URI
import java.time.Duration
import java.util.UUID
import java.util.function.Function
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.lang3.RandomStringUtils
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.glassfish.hk2.utilities.binding.AbstractBinder
import org.junit.After
import org.junit.Before
import org.junit.Test

class UnsubscribeRestTest {
    lateinit var serviceLocator: ServiceLocator
    val userService: UserService by hk2 { serviceLocator }
    val authenticationDAO: AuthenticationDAO by hk2 { serviceLocator }
    val ebeanServer: EbeanServer by hk2 { serviceLocator }
    val mailDeliveryService: MailDeliveryService by hk2 { serviceLocator }

    fun createAuthentication(domain: String): AuthenticationDBO {
        val accountDBO = AccountDBO(MailDomain(domain))
        ebeanServer.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
            login = domain,
            encryptedPassword = UUID.randomUUID().toString(),
            apiToken = UUID.randomUUID().toString(),
            role = AuthenticationRole.FRONTEND_ADMIN,
            account = accountDBO
        )
        ebeanServer.save(authenticationDBO)

        return authenticationDBO
    }

    fun createUser(): AuthenticationDBO {
        createAuthentication("example.com")
        val b4 = AddUserRequest.newBuilder()
            .setActive(true)
            .setAddApiToken(false)
            .setAuthenticationRole(FrontendUserRole.FRONTEND)
            .setMailDomain("example.com")
            .setPasswordAuth(PasswordAuth.newBuilder()
                .setLogin("foobar")
                .setPassword("123")
            )
            .build()
        val req = AddUserRequest.parseFrom(b4.toByteArray())

        val resp = userService.addFrontendUser(req)
        val user = resp.user

        val actual = authenticationDAO.getById(user.authId.toAuthenticationId())!!
        assertEquals(b4.active, user.active)
        assertEquals(b4.active, actual.active)

        assertEquals(b4.authenticationRole, user.authenticationRole)
        assertEquals(b4.authenticationRole, actual.role.toFrontendRole())

        assertNull(actual.apiToken)
        assertTrue(resp.apiToken.isEmpty())

        assertTrue(PasswordStorage.verifyPassword(b4.passwordAuth.password, actual.encryptedPassword!!))

        assertNull(actual.recipientOverride)
        assertFalse(user.hasRecipientOverride())

        assertEquals(b4.mailDomain, actual.account.mailDomain.mailDomain)
        return actual
    }

    fun startServer(): Server {
        val requestScoped: HttpRequestScopedDecorator by serviceLocator

        // Order matters!
        val serverBuilder = Server.builder()
        val exceptionHandler: RestExceptionHandlerFunction by serviceLocator

        val restDecoratorFunction = Function<HttpService, HttpService> { it.decorate(requestScoped) }
        for (restService in serviceLocator.getAllServices(RestService::class.java)) {
            serverBuilder.annotatedService("/", restService, restDecoratorFunction, exceptionHandler)
        }

        val server = serverBuilder
            // Grpc must be last
            .decorator(requestScoped)
            .requestTimeout(Duration.ofMinutes(1))
            .build()
        server.start()

        ServiceLocatorUtilities.bind(
            serviceLocator,
            object : AbstractBinder() {
                override fun configure() {
                    bind(
                        object : TrackingConfig {
                            override val linkSignKey = "lk,;sxjdfljkdskljhnfgdskjlhfrjhkl;fdsflijkfgdsjlkfdslkjfjklsd".toByteArray()
                            override val baseUrl: URI
                                get() = URI.create("http://localhost:${server.activeLocalPort()}/")
                        }
                    ).to(TrackingConfig::class.java)
                        .ranked(Int.MAX_VALUE)
                }
            }
        )
        return server
    }

    private lateinit var auth: AuthenticationDBO
    private lateinit var emailDBO: EmailDBO
    private lateinit var unsubscribeOneClickPostUri: URI
    private lateinit var unsubscribeClickUri: URI
    private lateinit var server: Server
    private val blockedEmail = Email("recip@example.net")
    private val fromEmail = Email("foo@example.com")
    private val okHttpClient = OkHttpClient.Builder()
        .followSslRedirects(true)
        .followRedirects(true)
        .build()

    @Before
    fun beforeTest() {
        serviceLocator = ServiceLocatorUtilities.bind(
            RandomStringUtils.randomAlphanumeric(10),
            TestBinder(),
            DatabaseBinder(),
            RestBinder(),
            WebBinder(),
            MinimalBinder(
                MailDeliveryService::class.java,
                UserService::class.java
            )
        )!!
        server = startServer()

        auth = createUser()

        val objectMapper = ObjectMapper()
        val transaction = EmailSendTransactionDBO(
            jsonRequest = objectMapper.createObjectNode(),
            fromEmail = fromEmail,
            authentication = auth
        )
        emailDBO = EmailDBO(
            recipient = NamedEmail(blockedEmail, "Foobar"),
            transaction = transaction,
            messageId = MessageId("2-id@example.com"),
            autoMailId = AutoMailId("2-id@example.net")
        )
        ebeanServer.save(emailDBO)

        unsubscribeOneClickPostUri = mailDeliveryService.createUnsubscribeOneClickPostLink(emailDBO.id, "")
        unsubscribeClickUri = mailDeliveryService.createUnsubscribeClickLink(emailDBO.id)
    }

    @After
    fun afterTest() {
        server.stop()
        serviceLocator.shutdown()
    }

    @Test
    fun `unsubscribe email`() {

        assertTrue {
            val request = Request.Builder()
                .url(unsubscribeOneClickPostUri.toURL())
                .post(
                    FormBody.Builder()
                        .add("List-Unsubscribe", "One-Click")
                        .build()
                )
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.assertSuccess()
            val blockedEmailDAO: BlockedEmailDAO by serviceLocator
            blockedEmailDAO.getBlockedEmails(auth.account)
                .first { it.recipientEmail == blockedEmail }
            blockedEmailDAO.unblock(auth.account, fromEmail, blockedEmail)
            blockedEmailDAO.getBlockedEmails(auth.account).isEmpty()
        }

        assertTrue {
            val request = Request.Builder()
                .url(unsubscribeClickUri.toURL())
                .get()
                .build()
            val response = okHttpClient.newCall(request).execute()
            response.assertSuccess()
            val blockedEmailDAO: BlockedEmailDAO by serviceLocator
            blockedEmailDAO.getBlockedEmails(auth.account)
                .first { it.recipientEmail == blockedEmail }
            blockedEmailDAO.unblock(auth.account, fromEmail, blockedEmail)
            blockedEmailDAO.getBlockedEmails(auth.account).isEmpty()
        }
    }

    fun Response.assertSuccess(): String =
        if (!isSuccessful) {
            fail(body?.string())
        } else {
            body?.string()!!
        }
}