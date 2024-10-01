package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.config.DatabaseConfig
import io.ebean.datasource.DataSourceConfig
import io.ebean.platform.postgres.PostgresPlatform
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.sql.Connection
import java.util.HashMap

class PostgresqlDataSourceProvider(override val di: DI) : DataSourceProvider, DIAware {
    private val databaseConfig: DatabaseConfig by instance()

    override fun provide(serverConfig: io.ebean.config.DatabaseConfig) {
        if (databaseConfig.sqlUrl.scheme != "postgres") {
            throw IllegalArgumentException("Not a postgres database")
        }

        val dataSourceConfig = DataSourceConfig()
        dataSourceConfig.minConnections = 1
        dataSourceConfig.maxConnections = 15
        dataSourceConfig.heartbeatSql = "select 1"
        dataSourceConfig.isAutoCommit = false
        dataSourceConfig.isolationLevel = Connection.TRANSACTION_READ_COMMITTED

        val dbUrl = databaseConfig.sqlUrl
        val userInfo = dbUrl.rawUserInfo
        val credentials = userInfo.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        dataSourceConfig.url = "jdbc:postgresql://${dbUrl.host}:${dbUrl.port}${dbUrl.path}"
        dataSourceConfig.username = credentials[0]
        dataSourceConfig.password = credentials[1]

        val customProperties = HashMap<String, String>()
        customProperties.put("ssl", "require")
        dataSourceConfig.customProperties = customProperties
        dataSourceConfig.driver = "org.postgresql.Driver"

        serverConfig.setDataSourceConfig(dataSourceConfig)

        serverConfig.databasePlatform = PostgresPlatform()
        serverConfig.isDdlGenerate = false
        serverConfig.isDdlRun = false
    }
}
