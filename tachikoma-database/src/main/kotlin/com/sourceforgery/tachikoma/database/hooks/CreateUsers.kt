package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.MQManager
import io.ebean.EbeanServer
import net.bytebuddy.utility.RandomString
import javax.inject.Inject

class CreateUsers
@Inject
private constructor(
        private val mqManager: MQManager,
        databaseConfig: DatabaseConfig
) : EbeanHook() {
    private val randomString = RandomString(40)
    private val mailDomain = databaseConfig.mailDomain

    override fun postStart(ebeanServer: EbeanServer) {
        ebeanServer
                .beginTransaction()
                .use {
                    ebeanServer
                            .find(AccountDBO::class.java)
                            .where()
                            .eq("mailDomain", mailDomain)
                            .findOne()
                            ?: also {
                                val account = AccountDBO(mailDomain)
                                LOGGER.error { "Creating new account and authentications for $mailDomain" }
                                ebeanServer.save(account)
                                mqManager.setupAccount(mailDomain)
                                createBackendAuthentication(ebeanServer, account)
                                createFrontendAuthentication(ebeanServer, account)
                                createIncomingEmail(ebeanServer, account)
                            }
                    it.commit()
                }
    }

    private fun createIncomingEmail(ebeanServer: EbeanServer, account: AccountDBO) {
        val incomingAddress = IncomingEmailAddressDBO(
                localPart = "",
                account = account
        )
        ebeanServer.save(incomingAddress)
    }

    private fun createFrontendAuthentication(ebeanServer: EbeanServer, account: AccountDBO) {
        val frontendAuthentication = AuthenticationDBO(
                account = account,
                apiToken = randomString.nextString(),
                role = AuthenticationRole.FRONTEND_ADMIN
        )
        ebeanServer.save(frontendAuthentication)
        LOGGER.error { "Creating new frontend api with login:password '$mailDomain:${frontendAuthentication.apiToken}'" }
        mqManager.setupAuthentication(
                mailDomain = account.mailDomain,
                authenticationId = frontendAuthentication.id,
                accountId = account.id
        )
    }

    private fun createBackendAuthentication(ebeanServer: EbeanServer, account: AccountDBO) {
        val backendAuthentication = AuthenticationDBO(
                apiToken = randomString.nextString(),
                role = AuthenticationRole.BACKEND,
                account = account
        )
        LOGGER.error { "Creating new backend api with login:password '$mailDomain:${backendAuthentication.apiToken}'" }
        ebeanServer.save(backendAuthentication)
    }

    companion object {
        val LOGGER = logger()
    }
}
