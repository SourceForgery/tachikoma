package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AccountDAOImpl
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAOImpl
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAOImpl
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAOImpl
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAOImpl
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAOImpl
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAOImpl
import com.sourceforgery.tachikoma.database.hooks.CreateSequence
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.database.server.DBObjectMapperImpl
import com.sourceforgery.tachikoma.database.server.EbeanServerFactory
import com.sourceforgery.tachikoma.database.server.InvokeCounter
import com.sourceforgery.tachikoma.database.server.LogEverything
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.RequestScoped
import com.sourceforgery.tachikoma.mq.MQManager
import io.ebean.EbeanServer
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.PerThread
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.hk2.internal.PerThreadContext
import org.glassfish.hk2.utilities.binding.AbstractBinder
import java.net.URI
import java.util.UUID
import javax.inject.Singleton

class Hk2TestBinder : AbstractBinder() {
    override fun configure() {
        bind(object : DatabaseConfig {
            override val databaseEncryptionKey = "asdadsadsadsadasdadasdasdadasasd"
            override val sqlUrl = URI.create("h2://sa@mem/tests-${UUID.randomUUID()}")
            override val timeDatabaseQueries = false
            override val wipeAndCreateDatabase = true
        })
                .to(DatabaseConfig::class.java)

        bindAsContract(TestHK2RequestContext::class.java)
                .to(HK2RequestContext::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(PerThreadContext::class.java)
                .to(PerThread::class.java)
                .to(object : TypeLiteral<Context<PerThread>>() {}.type)
                .`in`(Singleton::class.java)

        bindAsContract(TestConsumerFactoryImpl::class.java)
                .to(MQManager::class.java)
                .`in`(Singleton::class.java)

        bindFactory(EbeanServerFactory::class.java)
                .to(EbeanServer::class.java)
                .proxy(true)
                .proxyForSameScope(true)
                .`in`(Singleton::class.java)
        bindAsContract(BlockedEmailDAOImpl::class.java)
                .to(BlockedEmailDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(EmailDAOImpl::class.java)
                .to(EmailDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(AuthenticationDAOImpl::class.java)
                .to(AuthenticationDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(EmailStatusEventDAOImpl::class.java)
                .to(EmailStatusEventDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(IncomingEmailDAOImpl::class.java)
                .to(IncomingEmailDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(IncomingEmailAddressDAOImpl::class.java)
                .to(IncomingEmailAddressDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(AccountDAOImpl::class.java)
                .to(AccountDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(LogEverything::class.java)
                .to(InvokeCounter::class.java)
                .`in`(RequestScoped::class.java)
        bindAsContract(DBObjectMapperImpl::class.java)
                .to(DBObjectMapper::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(CreateSequence::class.java)
                .to(EbeanHook::class.java)
                .`in`(Singleton::class.java)
    }
}