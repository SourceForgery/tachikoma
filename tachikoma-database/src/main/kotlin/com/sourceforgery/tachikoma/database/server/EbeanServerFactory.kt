package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.upgrades.DatabaseUpgrade
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import io.ebean.EbeanServer
import io.ebean.config.EncryptKey
import io.ebean.config.EncryptKeyManager
import io.ebean.config.ServerConfig
import io.ebean.config.dbplatform.postgres.PostgresPlatform
import io.ebean.datasource.DataSourcePool
import java.sql.DriverManager
import javax.inject.Inject
import javax.sql.DataSource
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.glassfish.hk2.api.Factory
import org.glassfish.hk2.api.IterableProvider

class EbeanServerFactory
@Inject
private constructor(
    private val databaseConfig: DatabaseConfig,
    private val counter: InvokeCounter,
    private val dbObjectMapper: DBObjectMapper,
    private val ebeanHooks: IterableProvider<EbeanHook>,
    private val databaseUpgrades: IterableProvider<DatabaseUpgrade>,
    private val hK2RequestContext: HK2RequestContext,
    private val dataSourceProvider: DataSourceProvider
) : Factory<EbeanServer> {

    private inner class WrappedServerConfig : ServerConfig() {
        override fun setDataSource(originalDataSource: DataSource?) {
            if (this.databasePlatform is PostgresPlatform) {
                // Only do database upgrade on postgresql
                originalDataSource
                    ?.also { upgradeDatabase(it) }
            }

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
        for (serviceHandle in databaseUpgrades.handleIterator()) {
            val newVersion = serviceHandle.activeDescriptor.ranking
            if (newVersion == 0) {
                throw RuntimeException("Rank must be set on ${serviceHandle.activeDescriptor.implementationClass}")
            }
            if (newVersion < currentVersion) {
                dataSource.connection.use {
                    it.autoCommit = false
                    currentVersion = serviceHandle.service.run(it)
                    it.prepareStatement("UPDATE database_version SET version = ?")
                        .use {
                            it.setInt(1, currentVersion)
                            it.execute()
                        }
                    it.commit()
                }
            }
            serviceHandle.destroy()
        }
    }

    override fun provide(): EbeanServer {
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

        return hK2RequestContext.runInScope {
            val ebeanServer = io.ebean.EbeanServerFactory.create(serverConfig)
            ebeanHooks
                .handleIterator()
                .forEach {
                    it.service.postStart(ebeanServer)
                    it.destroy()
                }
            ebeanServer
        }
    }

    override fun dispose(instance: EbeanServer) {
        instance.shutdown(true, false)
    }
}