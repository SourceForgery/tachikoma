package com.sourceforgery.tachikoma.account

import com.expediagroup.graphql.client.jackson.types.OptionalInput
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.youcruit.graphql.client.test.generated.ChangeBaseUrl
import com.youcruit.graphql.client.test.generated.GetAccountData
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import org.kodein.di.instance
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AccountsGraphqlTest : AbstractGraphqlTest() {
    private val trackingConfig: TrackingConfig by instance()

    @Test
    fun `modify account`() {
        runBlocking {
            val mailDomain = MailDomain(RandomStringUtils.randomAlphabetic(10) + ".com")
            val account = AccountDBO(mailDomain)
            database.save(
                AuthenticationDBO(
                    account = account,
                    role = AuthenticationRole.FRONTEND_ADMIN,
                    apiToken = RandomStringUtils.randomAlphabetic(50),
                ),
            )
            database.refresh(account)
            val auth = account.authentications.first { it.role == AuthenticationRole.FRONTEND_ADMIN }

            val firstGet =
                client.execute(GetAccountData(GetAccountData.Variables(mailDomain.mailDomain))) {
                    addApitoken(auth)
                }

            assertEquals(trackingConfig.baseUrl.toASCIIString(), firstGet.data!!.accounts.getAccountData.baseUrl)

            val firstMod =
                client.execute(ChangeBaseUrl(ChangeBaseUrl.Variables(mailDomain.mailDomain, OptionalInput.Defined("https://foobar.com")))) {
                    addApitoken(auth)
                }
            assertEquals("https://foobar.com", firstMod.data!!.accounts.changeBaseUrl.baseUrl)

            database.refresh(account)
            assertEquals(URI("https://foobar.com"), account.baseUrl)

            val secondGet =
                client.execute(GetAccountData(GetAccountData.Variables(mailDomain.mailDomain))) {
                    addApitoken(auth)
                }
            assertEquals("https://foobar.com", secondGet.data!!.accounts.getAccountData.baseUrl)

            val secondMod =
                client.execute(ChangeBaseUrl(ChangeBaseUrl.Variables(mailDomain.mailDomain, OptionalInput.Defined(null)))) {
                    addApitoken(auth)
                }
            assertEquals(trackingConfig.baseUrl.toASCIIString(), secondMod.data!!.accounts.changeBaseUrl.baseUrl)

            database.refresh(account)
            assertNull(account.baseUrl)
        }
    }
}
