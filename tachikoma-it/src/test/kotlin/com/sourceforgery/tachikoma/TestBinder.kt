package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.auth.AuthenticationMock
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.database.server.DBObjectMapperImpl
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import com.sourceforgery.tachikoma.database.server.InvokeCounter
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageIdFactory
import com.sourceforgery.tachikoma.identifiers.MessageIdFactoryMock
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.mq.MQManager
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.TestConsumerFactoryImpl
import com.sourceforgery.tachikoma.mta.MTAEmailQueueService
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import java.net.URI
import java.time.Clock
import java.util.UUID
import javax.inject.Singleton
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.DynamicConfiguration
import org.glassfish.hk2.api.PerThread
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.hk2.internal.PerThreadContext
import org.glassfish.hk2.utilities.binding.AbstractBinder

class TestBinder(
    private vararg val attributes: TestAttribute
) : AbstractBinder() {
    private val databaseBinder = DatabaseBinder()

    override fun bind(configuration: DynamicConfiguration?) {
        super.bind(configuration)
        databaseBinder.bind(configuration)
    }

    override fun configure() {
        bind(object : TrackingConfig {
            override val linkSignKey = "lk,;sxjdfljkdskljhnfgdskjlhfrjhkl;fdsflijkfgdsjlkfdslkjfjklsd"
            override val baseUrl: URI = URI.create("http://localhost/")
        })
            .to(TrackingConfig::class.java)

        bindAsContract(PerThreadContext::class.java)
            .to(PerThread::class.java)
            .to(object : TypeLiteral<Context<PerThread>>() {}.type)
            .`in`(Singleton::class.java)

        bindAsContract(TrackingDecoderImpl::class.java)
            .to(TrackingDecoder::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(UnsubscribeDecoderImpl::class.java)
            .to(UnsubscribeDecoder::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(MQSequenceFactoryMock::class.java)
            .to(MQSequenceFactory::class.java)
            .`in`(Singleton::class.java)

        bindAsContract(MTAEmailQueueService::class.java)
            .`in`(Singleton::class.java)
        bind(Clock.systemUTC())
            .to(Clock::class.java)
        bindAsContract(TestHK2RequestContext::class.java)
            .to(HK2RequestContext::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(DatabaseTestConfig::class.java)
            .to(DatabaseConfig::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(TestConsumerFactoryImpl::class.java)
            .to(MQManager::class.java)
            .`in`(Singleton::class.java)

        bindAsContract(AuthenticationMock::class.java)
            .to(Authentication::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(MQSenderMock::class.java)
            .to(MQSender::class.java)
            .`in`(Singleton::class.java)

        bindAsContract(DAOHelper::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(JobMessageFactory::class.java)
            .`in`(Singleton::class.java)
        bindAsContract(MessageIdFactoryMock::class.java)
            .to(MessageIdFactory::class.java)
            .`in`(Singleton::class.java)

        bind(object : InvokeCounter {
            override fun inc(sql: String?, millis: Long) {
                // Do nothing
            }
        })
            .to(InvokeCounter::class.java)

        val dataSourceProvider = if (attributes.contains(TestAttribute.POSTGRESQL)) {
            PostgresqlEmbeddedDataSourceProvider::class.java
        } else {
            H2DataSourceProvider::class.java
        }
        bindAsContract(dataSourceProvider)
            .to(DataSourceProvider::class.java)
            .`in`(Singleton::class.java)
            .ranked(1)

        bindAsContract(DBObjectMapperImpl::class.java)
            .to(DBObjectMapper::class.java)
            .`in`(Singleton::class.java)
    }
}

enum class TestAttribute {
    POSTGRESQL
}

private class DatabaseTestConfig : DatabaseConfig {
    override val mailDomain: MailDomain = MailDomain("example.net")
    override val databaseEncryptionKey = "asdadsadsadsadasdadasdasdadasasd"
    override val sqlUrl = URI.create("h2://sa@mem/tests-${UUID.randomUUID()}")
    override val timeDatabaseQueries = false
    override val createDatabase = true
}