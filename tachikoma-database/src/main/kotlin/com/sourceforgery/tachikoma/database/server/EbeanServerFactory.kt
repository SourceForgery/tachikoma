package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.hooks.DatabaseUpgrade
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import io.ebean.EbeanServer
import io.ebean.config.EncryptKey
import io.ebean.config.EncryptKeyManager
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.h2.H2Platform
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.avaje.datasource.DataSourceConfig
import org.avaje.datasource.DataSourcePool
import org.glassfish.hk2.api.Factory
import org.glassfish.hk2.api.IterableProvider
import java.net.URI
import java.sql.Connection
import java.sql.DriverManager
import java.util.HashMap
import javax.inject.Inject
import javax.sql.DataSource

internal class EbeanServerFactory
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig,
        private val counter: InvokeCounter,
        private val dbObjectMapper: DBObjectMapper,
        private val ebeanHooks: IterableProvider<EbeanHook>,
        private val databaseUpgrades: IterableProvider<DatabaseUpgrade>,
        private val hK2RequestContext: HK2RequestContext
) : Factory<EbeanServer> {

    private inner class LoggingServerConfig : ServerConfig() {
        override fun setDataSource(originalDataSource: DataSource?) {
            if (this.databasePlatform is PostgresPlatform) {
                // Only do database upgrade on postgresql
                originalDataSource
                        ?.also { upgradeDatabase(it) }
            }
            val loggingDataSource =
                    when (originalDataSource) {
                        null -> null
                        is DataSourcePool -> LoggingDataSourcePool(
                                originalDataSourcePool = originalDataSource,
                                counter = counter
                        )
                        else -> LoggingDataSource(
                                originalDataSource = originalDataSource,
                                counter = counter
                        )
                    }
            super.setDataSource(loggingDataSource)
        }

        private fun upgradeDatabase(dataSource: DataSource) {
            var currentVersion = 0
            for (serviceHandle in databaseUpgrades.handleIterator()) {
                val newVersion = serviceHandle.activeDescriptor.ranking
                if (newVersion == 0) {
                    throw RuntimeException("Rank must be set on ${serviceHandle.activeDescriptor.implementationClass}")
                }
                if (newVersion < currentVersion) {
                    dataSource.connection.use {
                        it.autoCommit = false
                        currentVersion = serviceHandle.service.run(it)
                        val prepareStatement = it.prepareStatement("UPDATE database_version SET version = ?")
                        prepareStatement.setInt(1, currentVersion)
                        prepareStatement.execute()
                        it.commit()
                    }
                }
                serviceHandle.destroy()
            }
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
        serverConfig.addPackage("com.sourceforgery.tachikoma.database.objects")
        serverConfig.dataSourceConfig = dataSourceConfig
        serverConfig.isDefaultServer = false
        serverConfig.isRegister = false
        serverConfig.encryptKeyManager = EncryptKeyManager { _, _ -> EncryptKey { databaseConfig.databaseEncryptionKey } }
        serverConfig.objectMapper = dbObjectMapper
        serverConfig.databaseSequenceBatchSize = 100
        when (databaseConfig.sqlUrl.scheme) {
            "h2" -> {
                dataSourceConfig.driver = "org.h2.Driver"
                serverConfig.databasePlatform = H2Platform()
                if (databaseConfig.createDatabase) {
                    // Really only for tests
                    serverConfig.isDdlCreateOnly = true
                    serverConfig.isDdlGenerate = true
                    serverConfig.isDdlRun = true
                }
            }
            "postgres" -> {
                dataSourceConfig.driver = "org.postgresql.Driver"
                serverConfig.databasePlatform = PostgresPlatform()
                serverConfig.isDdlGenerate = false
                serverConfig.isDdlRun = false
            }
            else -> throw IllegalArgumentException("Don't know anything about the database ${databaseConfig.sqlUrl.scheme}.")
        }

        return hK2RequestContext.runInScope {
            val ebeanServer = io.ebean.EbeanServerFactory.create(serverConfig)
            ebeanHooks.forEach { it.postStart(ebeanServer) }
            ebeanServer
        }
    }

    override fun dispose(instance: EbeanServer) {
        instance.shutdown(true, false)
    }

    companion object {
        private const val MIN_PSQL_CONNECTIONS = 1
        private const val MAX_PSQL_CONNECTIONS = 15

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