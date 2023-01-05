package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.upgrades.DatabaseUpgrade
import com.sourceforgery.tachikoma.logging.InvokeCounter
import io.ebean.Database
import io.ebean.DatabaseFactory
import io.ebean.config.EncryptKey
import io.ebean.config.EncryptKeyManager
import io.ebean.datasource.DataSourcePool
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.instance
import org.kodein.di.provider
import java.sql.DriverManager
import javax.sql.DataSource

class EbeanServerFactory(override val di: DI) : DIAware {
    private val databaseConfig: DatabaseConfig by instance()
    private val counter by provider<InvokeCounter>()
    private val dbObjectMapper: DBObjectMapper by instance()
    private val ebeanHooks by allInstances<EbeanHook>()
    private val databaseUpgrades by allInstances<DatabaseUpgrade>()
    private val dataSourceProvider: DataSourceProvider by instance()

    private inner class WrappedServerConfig : io.ebean.config.DatabaseConfig() {
        override fun setDataSource(originalDataSource: DataSource?) {
            originalDataSource
                ?.also { upgradeDatabase(it) }

            if (databaseConfig.timeDatabaseQueries) {
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
            } else {
                super.setDataSource(originalDataSource)
            }
        }
    }

    private fun upgradeDatabase(dataSource: DataSource) {
        var currentVersion = 0
        for (service in databaseUpgrades.sortedByDescending { it.newVersion }) {
            val newVersion = service.newVersion
            if (newVersion < currentVersion) {
                dataSource.connection.use {
                    it.autoCommit = false
                    currentVersion = service.run(it)
                    it.prepareStatement("UPDATE database_version SET version = ?")
                        .use {
                            it.setInt(1, currentVersion)
                            it.execute()
                        }
                    it.commit()
                }
            }
        }
    }

    fun provide(): Database {
        DriverManager.setLogWriter(IoBuilder.forLogger("DriverManager").setLevel(Level.DEBUG).buildPrintWriter())

        val serverConfig = WrappedServerConfig()
        serverConfig.name = "tachikoma"
        serverConfig.addPackage("com.sourceforgery.tachikoma.database.objects")
        serverConfig.isDefaultServer = false
        serverConfig.isRegister = false
        serverConfig.encryptKeyManager = EncryptKeyManager { _, _ -> EncryptKey { databaseConfig.databaseEncryptionKey } }
        serverConfig.objectMapper = dbObjectMapper.objectMapper
        serverConfig.databaseSequenceBatchSize = 100
        serverConfig.lazyLoadBatchSize = 100
        dataSourceProvider.provide(serverConfig)

        val ebeanServer = DatabaseFactory.create(serverConfig)
        ebeanHooks
            .forEach {
                it.postStart(ebeanServer)
            }
        return ebeanServer
    }
}
