package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.database.auth.InternalCreateUserService
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import io.ebean.Database
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class CreateUsers(override val di: DI) : DIAware {
    private val databaseConfig: DatabaseConfig by instance()
    private val trackingConfig: TrackingConfig by instance()
    private val accountDAO: AccountDAO by instance()
    private val ebeanServer: Database by instance()
    private val internalCreateUserService: InternalCreateUserService by instance()

    private val mailDomains = databaseConfig.mailDomains

    fun createUsers() {
        ebeanServer
            .beginTransaction()
            .use {
                for (mailDomain in mailDomains) {
                    accountDAO.get(mailDomain)
                        ?: also {
                            val account = internalCreateUserService.createAccount(mailDomain)
                            LOGGER.error { "Creating new account and authentications for $mailDomain" }
                            val backendAuth = internalCreateUserService.createBackendAuthentication(account)
                            val uri = trackingConfig.baseUrl
                            LOGGER.error { "Creating new backend api with login:password 'gproto+${uri.scheme}://$mailDomain:${backendAuth.apiToken}@${uri.host}'" }
                            val frontendAuth =
                                internalCreateUserService.createFrontendAuthentication(
                                    account = account,
                                    role = AuthenticationRole.FRONTEND_ADMIN,
                                    addApiToken = true,
                                )
                            LOGGER.error { "Creating new frontend api with login:password 'gproto+${uri.scheme}://$mailDomain:${frontendAuth.apiToken}@${uri.host}:${uri.port}'" }
                            createIncomingEmail(ebeanServer, account)
                        }
                }
                it.commit()
            }
    }

    private fun createIncomingEmail(
        ebeanServer: Database,
        account: AccountDBO,
    ) {
        val incomingAddress =
            IncomingEmailAddressDBO(
                localPart = "",
                account = account,
            )
        ebeanServer.save(incomingAddress)
    }

    companion object {
        val LOGGER = logger()
    }
}
