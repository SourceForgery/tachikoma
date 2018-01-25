package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.MQManager
import io.ebean.EbeanServer
import org.apache.commons.lang3.RandomStringUtils
import javax.inject.Inject

class CreateUsers
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig,
        private val mqManager: MQManager
) : EbeanHook() {
    override fun postStart(ebeanServer: EbeanServer) {
        if (databaseConfig.createDatabase) {
            ebeanServer.find(AccountDBO::class.java)
                    .where()
                    .eq("mailDomain", MAIL_DOMAIN)
                    .findOne()
            ?: let {

                val account = AccountDBO(MAIL_DOMAIN)
                LOGGER.error { "Creating new account and authentications for $MAIL_DOMAIN" }
                ebeanServer.save(account)
                mqManager.setupAccount(MAIL_DOMAIN)
                createBackendAuthentication(ebeanServer, account)
                createFrontendAuthentication(ebeanServer, account)
                createIncomingEmail(ebeanServer, account)
            }
        }
    }

    private fun createIncomingEmail(ebeanServer: EbeanServer, account: AccountDBO) {
        val incomingAddress = IncomingEmailAddressDBO(
                localPart = null,
                mailDomain = MAIL_DOMAIN,
                account = account
        )
        ebeanServer.save(incomingAddress)
    }

    private fun createFrontendAuthentication(ebeanServer: EbeanServer, account: AccountDBO) {
        val frontendAuthentication = AuthenticationDBO(
                account = account,
                apiToken = RandomStringUtils.randomAlphanumeric(40),
                role = AuthenticationRole.FRONTEND_ADMIN
        )
        ebeanServer.save(frontendAuthentication)
        LOGGER.error { "Creating new frontend api with login:password '$MAIL_DOMAIN:${frontendAuthentication.apiToken}'" }
        mqManager.setupAuthentication(
                mailDomain = account.mailDomain,
                authenticationId = frontendAuthentication.id,
                accountId = account.id
        )
    }

    private fun createBackendAuthentication(ebeanServer: EbeanServer, account: AccountDBO) {
        val backendAuthentication = AuthenticationDBO(
                apiToken = RandomStringUtils.randomAlphanumeric(40),
                role = AuthenticationRole.BACKEND,
                account = account
        )
        LOGGER.error { "Creating new backend api with login:password '$MAIL_DOMAIN:${backendAuthentication.apiToken}'" }
        ebeanServer.save(backendAuthentication)
    }

    companion object {
        val LOGGER = logger()
        val MAIL_DOMAIN = MailDomain(
                System.getenv("MAIL_DOMAIN") ?: "example.net"
        )
    }
}
