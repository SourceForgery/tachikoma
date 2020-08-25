package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.h2.H2Platform
import io.ebean.datasource.DataSourceConfig
import java.sql.Connection
import java.util.UUID
import org.kodein.di.DI
import org.kodein.di.DIAware

class H2DataSourceProvider(override val di: DI) : DataSourceProvider, DIAware {
    override fun provide(serverConfig: ServerConfig) {
        val dataSourceConfig = DataSourceConfig()
        dataSourceConfig.heartbeatSql = "select 1"
        dataSourceConfig.isAutoCommit = false
        dataSourceConfig.isolationLevel = Connection.TRANSACTION_READ_COMMITTED

        dataSourceConfig.driver = "org.h2.Driver"
        dataSourceConfig.url = "jdbc:h2:mem:tests-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"
        dataSourceConfig.username = "sa"
        dataSourceConfig.password = "blank"

        serverConfig.dataSourceConfig = dataSourceConfig
        serverConfig.databasePlatform = H2Platform()
        serverConfig.isDdlCreateOnly = true
        serverConfig.isDdlGenerate = true
        serverConfig.isDdlRun = true
    }
}
