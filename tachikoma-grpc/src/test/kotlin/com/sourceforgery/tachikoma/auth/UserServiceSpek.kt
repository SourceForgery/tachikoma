package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ApiToken
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.grpc.frontend.toFrontendRole
import com.sourceforgery.tachikoma.hk2.located
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.users.UserService
import io.ebean.EbeanServer
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class UserServiceSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    val userService: () -> UserService = located { serviceLocator }
    val authenticationDAO: () -> AuthenticationDAO = located { serviceLocator }
    val ebeanServer: () -> EbeanServer = located { serviceLocator }

    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder(), DatabaseBinder())!!
    }

    afterEachTest {
        serviceLocator.shutdown()
    }

    fun createAuthentication(domain: String): AuthenticationDBO {
        val accountDBO = AccountDBO(MailDomain(domain))
        ebeanServer().save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
                login = domain,
                encryptedPassword = UUID.randomUUID().toString(),
                apiToken = UUID.randomUUID().toString(),
                role = AuthenticationRole.FRONTEND_ADMIN,
                account = accountDBO
        )
        ebeanServer().save(authenticationDBO)

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

        val resp = userService().addFrontendUser(req)
        val user = resp.user

        val actual = authenticationDAO().getById(user.authId.toAuthenticationId())!!
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

    describe("UserServiceSpec", {
        it("create & modify user", {
            val newUser = createUser()
            val before = ModifyUserRequest.newBuilder()
                    .setActive(false)
                    .setApiToken(ApiToken.RESET_API_TOKEN)
                    .build()
            val oldApiToken = newUser.apiToken
            val oldMailDomain = newUser.account.mailDomain
            val resp = userService().modifyFrontendUser(before, newUser)

            val user = resp.user
            val actual = authenticationDAO().getById(user.authId.toAuthenticationId())!!
            assertEquals(before.active, user.active)
            assertEquals(before.active, actual.active)

            assertEquals(before.authenticationRole, user.authenticationRole)
            assertEquals(before.authenticationRole, actual.role.toFrontendRole())

            assertEquals(actual.apiToken, resp.apiToken)

            assertNull(actual.recipientOverride)
            assertFalse(user.hasRecipientOverride())

            assertEquals(oldMailDomain, actual.account.mailDomain)

            assertNotEquals(oldApiToken, actual.apiToken)
        })
    })
})
