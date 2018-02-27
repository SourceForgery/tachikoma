package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.h2.H2Platform
import org.avaje.datasource.DataSourceConfig
import java.sql.Connection
import java.util.UUID
import javax.inject.Inject

class H2DataSourceProvider
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig
) : DataSourceProvider {
    override fun provide(serverConfig: ServerConfig) {
        val dataSourceConfig = DataSourceConfig()
        dataSourceConfig.heartbeatSql = "select 1"
        dataSourceConfig.isAutoCommit = false
        dataSourceConfig.isolationLevel = Connection.TRANSACTION_READ_COMMITTED

        dataSourceConfig.driver = "org.h2.Driver"
        dataSourceConfig.url = "jdbc:h2:mem:tests-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        dataSourceConfig.username = "sa"
        dataSourceConfig.password = "blank"

        System.err.println("DataSourceProvider.provide ${dataSourceConfig.url}, databaseConfig: ${databaseConfig.sqlUrl}")

        serverConfig.dataSourceConfig = dataSourceConfig
        serverConfig.databasePlatform = H2Platform()
        serverConfig.isDdlCreateOnly = true
        serverConfig.isDdlGenerate = true
        serverConfig.isDdlRun = true
    }
}