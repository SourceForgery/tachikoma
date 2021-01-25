package com.sourceforgery.tachikoma.rest.unsubscribe

import com.fasterxml.jackson.databind.ObjectMapper
import com.linecorp.armeria.server.Server
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.auth.AuthenticationMock
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.commonModule
import com.sourceforgery.tachikoma.config.TrackingConfig
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
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.rest.restModule
import com.sourceforgery.tachikoma.testModule
import com.sourceforgery.tachikoma.users.UserService
import com.sourceforgery.tachikoma.webserver.hk2.webModule
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import io.ebean.EbeanServer
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.net.URI
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class UnsubscribeRestTest : DIAware {

    override val di = DI {
        importOnce(commonModule)
        importOnce(webModule)
        importOnce(restModule)
        bind<Authentication>(overrides = true) with singleton { AuthenticationMock() }
        importOnce(testModule(), allowOverride = true)
        bind<MailDeliveryService>() with singleton { MailDeliveryService(di) }
        bind<UserService>() with singleton { UserService(di) }
        bind<TrackingConfig>() with instance(
            object : TrackingConfig {
                override val linkSignKey = "lk,;sxjdfljkdskljhnfgdskjlhfrjhkl;fdsflijkfgdsjlkfdslkjfjklsd".toByteArray()
                override val baseUrl: URI
                    get() = URI.create("http://localhost:${server.activeLocalPort()}/")
            }
        )
    }

    val userService: UserService by instance()
    val authenticationDAO: AuthenticationDAO by instance()
    val ebeanServer: EbeanServer by instance()
    val mailDeliveryService: MailDeliveryService by instance()
    val blockedEmailDAO: BlockedEmailDAO by instance()
    val restServices: List<RestService> by allInstances()
    val exceptionHandler: RestExceptionHandlerFunction by instance()

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
            .setPasswordAuth(
                PasswordAuth.newBuilder()
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

        // Order matters!
        val serverBuilder = Server.builder()

        for (restService in restServices) {
            serverBuilder.annotatedService("/", restService, exceptionHandler)
        }

        val server = serverBuilder
            // Grpc must be last
            .requestTimeout(Duration.ofMinutes(1))
            .build()
        server.start()

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
        .callTimeout(Duration.ofSeconds(600))
        .readTimeout(Duration.ofSeconds(600))
        .connectTimeout(Duration.ofSeconds(600))
        .writeTimeout(Duration.ofSeconds(600))
        .build()

    @Before
    fun beforeTest() {

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
