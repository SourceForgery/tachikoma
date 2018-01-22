package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MQManager
import io.ebean.EbeanServer
import javax.inject.Inject

class CreateUsers
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig,
        private val mqManager: MQManager
) : EbeanHook() {
    override fun postStart(ebeanServer: EbeanServer) {
        if (databaseConfig.wipeAndCreateDatabase) {
            val account = AccountDBO(MAIL_DOMAIN)
            ebeanServer.save(account)
            mqManager.setupAccount(MAIL_DOMAIN)
            createBackendAuthentication(ebeanServer, account)
            createFrontendAuthentication(ebeanServer, account)
            createIncomingEmail(ebeanServer, account)
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
                apiToken = "Oufeing2ieth2aequie2ia2ahc3yoonaiw5iey5xifuxoo4tai"
        )
        ebeanServer.save(frontendAuthentication)
        mqManager.setupAuthentication(
                mailDomain = account.mailDomain,
                authenticationId = frontendAuthentication.id,
                accountId = account.id
        )
    }

    private fun createBackendAuthentication(ebeanServer: EbeanServer, account: AccountDBO) {
        val backendAuthentication = AuthenticationDBO(
                apiToken = "oodua5yai9Pah5ook3wah4hahqu4IeK0iung8ou5Cho4Doonee",
                backend = true,
                account = account
        )
        ebeanServer.save(backendAuthentication)
    }

    companion object {
        val MAIL_DOMAIN = MailDomain("example.net")
    }
}
