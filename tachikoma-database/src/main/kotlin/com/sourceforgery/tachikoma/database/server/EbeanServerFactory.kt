package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.logging.logger
import io.ebean.EbeanServer
import io.ebean.config.ServerConfig
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.avaje.datasource.DataSourceConfig
import org.avaje.datasource.DataSourcePool
import org.glassfish.hk2.api.Factory
import java.net.URI
import java.sql.Connection
import java.sql.DriverManager
import java.util.HashMap
import javax.inject.Inject
import javax.sql.DataSource

class EbeanServerFactory @Inject constructor(
        private val databaseConfig: DatabaseConfig,
        private val counter: InvokeCounter
) : Factory<EbeanServer> {

    private inner class LoggingServerConfig : ServerConfig() {
        override fun setDataSource(originalDataSource: DataSource?) {
            super.setDataSource(
                    if (originalDataSource == null) {
                        null
                    } else if (originalDataSource is DataSourcePool) {
                        LoggingDataSourcePool(
                                originalDataSourcePool = originalDataSource,
                                sandbox = databaseConfig.timeDatabaseQueries,
                                counter = counter
                        )
                    } else {
                        LoggingDataSource(
                                originalDataSource = originalDataSource,
                                sandbox = databaseConfig.timeDatabaseQueries,
                                counter = counter
                        )
                    }
            )
        }
    }

    override fun provide(): EbeanServer {
        DriverManager.setLogWriter(IoBuilder.forLogger("DriverManager").setLevel(Level.DEBUG).buildPrintWriter())

        val serverConfig =
                if (databaseConfig.timeDatabaseQueries) {
                    LoggingServerConfig()
                } else {
                    ServerConfig()
                }
        serverConfig.name = "tachikoma"
        val dataSourceConfig = DataSourceConfig()
        parseDataSourceConfigFromURL(dataSourceConfig, databaseConfig.sqlUrl)
        dataSourceConfig.minConnections = MIN_PSQL_CONNECTIONS
        dataSourceConfig.maxConnections = MAX_PSQL_CONNECTIONS
        dataSourceConfig.heartbeatSql = "select 1"
        dataSourceConfig.isAutoCommit = false
        dataSourceConfig.isolationLevel = Connection.TRANSACTION_READ_COMMITTED
        serverConfig.addPackage("com.sourceforgery.tachikoma.database.sql.objects")
        serverConfig.dataSourceConfig = dataSourceConfig
        serverConfig.isDefaultServer = false
        serverConfig.isRegister = false
        serverConfig.objectMapper = createObjectMapper()
        if (databaseConfig.testDatabase) {
            serverConfig.isDdlCreateOnly = false
            serverConfig.isDdlGenerate = true
            serverConfig.isDdlRun = true
        }
        return io.ebean.EbeanServerFactory.create(serverConfig)
    }

    private fun createObjectMapper() =
            ObjectMapper()
                    .registerModule(JavaTimeModule())
                    .registerKotlinModule()

    override fun dispose(instance: EbeanServer) {
        instance.shutdown(true, false)
    }

    companion object {
        private val MIN_PSQL_CONNECTIONS = 1
        private val MAX_PSQL_CONNECTIONS = 15

        fun parseDataSourceConfigFromURL(config: DataSourceConfig, dbUrl: URI) {
            val scheme = dbUrl.scheme
            if (scheme == "postgres") {
                val userInfo = dbUrl.rawUserInfo
                val credentials = userInfo.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                config.url = "jdbc:postgresql://${dbUrl.host}:${dbUrl.port}${dbUrl.path}"
                config.username = credentials[0]
                config.password = credentials[1]
                val customProperties = HashMap<String, String>()
                customProperties.put("ssl", "true")
                customProperties.put("sslfactory", "org.postgresql.ssl.NonValidatingFactory")
                config.customProperties = customProperties
            } else if (scheme == "h2") {
                config.url = "jdbc:h2:mem:${dbUrl.path};DB_CLOSE_DELAY=-1"
                config.username = "sa"
                config.password = "blank"
            }
        }
    }
}