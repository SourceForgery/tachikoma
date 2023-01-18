package com.sourceforgery.tachikoma

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import io.ebean.config.DatabaseConfig
import io.ebean.platform.postgres.PostgresPlatform
import org.kodein.di.DI
import org.kodein.di.DIAware

class PostgresqlEmbeddedDataSourceProvider(override val di: DI) : DataSourceProvider, DIAware {
    override fun provide(serverConfig: DatabaseConfig) {
        val pg = EmbeddedPostgres.builder()
            .setServerConfig("listen_addresses", "127.0.0.1")
            .setOutputRedirector(ProcessBuilder.Redirect.PIPE)
            .start()
        serverConfig.databasePlatform = PostgresPlatform()
        serverConfig.isDdlGenerate = false
        serverConfig.isDdlRun = false
        serverConfig.dataSource = pg.postgresDatabase
    }
}
