package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageIdFactory
import com.sourceforgery.tachikoma.identifiers.MessageIdFactoryMock
import com.sourceforgery.tachikoma.kodein.DatabaseSessionKodeinScope
import com.sourceforgery.tachikoma.logging.InvokeCounter
import com.sourceforgery.tachikoma.mq.MQManager
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.TestConsumerFactoryImpl
import com.sourceforgery.tachikoma.mta.MTAEmailQueueService
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeConfig
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.scoped
import org.kodein.di.singleton
import java.net.URI
import java.util.UUID

fun testModule(vararg attributes: TestAttribute) =
    DI.Module("test") {
        importOnce(databaseModule)
        importOnce(decoderModule)

        bind<MQSequenceFactory>() with singleton { MQSequenceFactoryMock(di) }
        bind<MTAEmailQueueService>() with singleton { MTAEmailQueueService(di) }
        bind<Clocker>() with singleton { Clocker() }
        bind<DatabaseTestConfig>() with singleton { DatabaseTestConfig() }
        bind<MQManager>() with singleton { TestConsumerFactoryImpl() }

        bind<MQSender>() with singleton { MQSenderMock(di) }
        bind<DAOHelper>() with singleton { DAOHelper(di) }

        bind<MessageIdFactory>() with singleton { MessageIdFactoryMock() }

        bind<InvokeCounter>(overrides = true) with
            scoped(DatabaseSessionKodeinScope).singleton {
                object : InvokeCounter {
                    override fun inc(
                        sql: String?,
                        millis: Long,
                    ) {
                    }
                }
            }

        if (TestAttribute.POSTGRESQL in attributes) {
            bind<DataSourceProvider>(overrides = true) with singleton { PostgresqlEmbeddedDataSourceProvider(di) }
            importOnce(databaseUpgradesModule)
        } else {
            bind<DataSourceProvider>(overrides = true) with singleton { H2DataSourceProvider(di) }
            bind<H2DatabaseUpgrade>() with singleton { H2DatabaseUpgrade() }
        }
    }

enum class TestAttribute {
    POSTGRESQL,
}

private class DatabaseTestConfig : DatabaseConfig, DebugConfig, TrackingConfig, UnsubscribeConfig {
    override val mailDomains: List<MailDomain> = listOf(MailDomain("example.net"))
    override val databaseEncryptionKey = "asdadsadsadsadasdadasdasdadasasd"
    override val sqlUrl = URI.create("h2://sa@mem/tests-${UUID.randomUUID()}")
    override val timeDatabaseQueries = false
    override val linkSignKey = "lk,;sxjdfljkdskljhnfgdskjlhfrjhkl;fdsflijkfgdsjlkfdslkjfjklsd".toByteArray()
    override val baseUrl: URI = URI.create("https://localhost/")
    override val unsubscribeDomainOverride = MailDomain("example.net")
    override val sendDebugData: Boolean = true
}
