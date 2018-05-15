package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.config.DatabaseConfig
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import org.avaje.datasource.DataSourceConfig
import java.lang.IllegalArgumentException
import java.sql.Connection
import java.util.HashMap
import javax.inject.Inject

class PostgresqlDataSourceProvider
@Inject
private constructor(
    private val databaseConfig: DatabaseConfig
) : DataSourceProvider {
    override fun provide(serverConfig: ServerConfig) {
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
        customProperties.put("ssl", "true")
        customProperties.put("sslfactory", "org.postgresql.ssl.NonValidatingFactory")
        dataSourceConfig.customProperties = customProperties
        dataSourceConfig.driver = "org.postgresql.Driver"

        serverConfig.dataSourceConfig = dataSourceConfig

        serverConfig.databasePlatform = PostgresPlatform()
        serverConfig.isDdlGenerate = false
        serverConfig.isDdlRun = false
    }
}