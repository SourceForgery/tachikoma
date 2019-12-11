package com.sourceforgery.tachikoma

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import javax.inject.Inject

class PostgresqlEmbeddedDataSourceProvider
@Inject
private constructor() : DataSourceProvider {
    override fun provide(serverConfig: ServerConfig) {
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