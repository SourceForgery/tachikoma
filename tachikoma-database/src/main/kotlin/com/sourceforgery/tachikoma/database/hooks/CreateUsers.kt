package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.auth.InternalCreateUserService
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import io.ebean.EbeanServer
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

class CreateUsers
@Inject
private constructor(
    databaseConfig: DatabaseConfig,
    private val accountDAO: AccountDAO,
    private val ebeanServer: EbeanServer,
    private val internalCreateUserService: InternalCreateUserService
) {
    private val mailDomain = databaseConfig.mailDomain

    fun createUsers() {
        ebeanServer
            .beginTransaction()
            .use {
                accountDAO.getByMailDomain(mailDomain)
                    ?: also {
                        val account = internalCreateUserService.createAccount(mailDomain)
                        LOGGER.error { "Creating new account and authentications for $mailDomain" }
                        val backendAuth = internalCreateUserService.createBackendAuthentication(account)
                        LOGGER.error { "Creating new backend api with login:password '$mailDomain:${backendAuth.apiToken}'" }
                        val frontendAuth = internalCreateUserService.createFrontendAuthentication(
                            account = account,
                            role = AuthenticationRole.FRONTEND_ADMIN,
                            addApiToken = true
                        )
                        LOGGER.error { "Creating new frontend api with login:password '$mailDomain:${frontendAuth.apiToken}'" }
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

    companion object {
        val LOGGER = logger()
    }
}