package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.common.PasswordStorage
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.AddUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ApiToken
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.ModifyUserRequest
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.PasswordAuth
import com.sourceforgery.tachikoma.grpc.frontend.toAuthenticationId
import com.sourceforgery.tachikoma.grpc.frontend.toFrontendRole
import com.sourceforgery.tachikoma.hk2.get
import com.sourceforgery.tachikoma.users.UserService
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
class UserServiceSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var userService: UserService
    lateinit var authenticationDAO: AuthenticationDAO
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder(), DatabaseBinder())!!
        userService = serviceLocator.get()
        authenticationDAO = serviceLocator.get()
    }

    fun createUser(): AuthenticationDBO {
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
        assertNull(resp.apiToken)

        assertTrue(PasswordStorage.verifyPassword(b4.passwordAuth.password, actual.encryptedPassword!!))

        assertNull(actual.recipientOverride)
        assertFalse(user.hasRecipientOverride())

        assertEquals(b4.mailDomain, actual.account.mailDomain.mailDomain)
        return actual
    }

    it("create & modify user", {
        val newUser = createUser()
        val b4 = ModifyUserRequest.newBuilder()
                .setActive(false)
                .setApiToken(ApiToken.RESET_API_TOKEN)
                .build()
        userService.modifyFrontendUser(b4)
    })
})
