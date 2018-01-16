package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class CreateUsers
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig
) : EbeanHook() {
    override fun postStart(ebeanServer: EbeanServer) {
        if (databaseConfig.wipeAndCreateDatabase) {
            createBackendAccount(ebeanServer)
            createFrontendAccount(ebeanServer)
        }
    }

    private fun createFrontendAccount(ebeanServer: EbeanServer) {
        val frontendAccount = AccountDBO()
        ebeanServer.save(frontendAccount)
        val frontendAuthentication = AuthenticationDBO(
                account = frontendAccount,
                apiToken = "Oufeing2ieth2aequie2ia2ahc3yoonaiw5iey5xifuxoo4tai"
        )
        ebeanServer.save(frontendAuthentication)
    }

    private fun createBackendAccount(ebeanServer: EbeanServer) {
        val backendAuthentication = AuthenticationDBO(
                apiToken = "oodua5yai9Pah5ook3wah4hahqu4IeK0iung8ou5Cho4Doonee",
                backend = true
        )
        ebeanServer.save(backendAuthentication)
    }
}
