package com.sourceforgery.tachikoma

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
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.database.hooks.EbeanHook
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.database.server.DBObjectMapperImpl
import com.sourceforgery.tachikoma.database.server.EbeanServerFactory
import com.sourceforgery.tachikoma.database.server.InvokeCounter
import com.sourceforgery.tachikoma.database.server.LogEverything
import com.sourceforgery.tachikoma.database.upgrades.DatabaseUpgrade
import com.sourceforgery.tachikoma.database.upgrades.Version1
import com.sourceforgery.tachikoma.database.upgrades.Version2
import com.sourceforgery.tachikoma.database.upgrades.Version3
import com.sourceforgery.tachikoma.database.upgrades.Version4
import com.sourceforgery.tachikoma.database.upgrades.Version5
import com.sourceforgery.tachikoma.hk2.RequestScoped
import io.ebean.EbeanServer
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class DatabaseBinder : AbstractBinder() {
    override fun configure() {
        bindFactory(EbeanServerFactory::class.java)
                .to(EbeanServer::class.java)
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
        bindEbeanHooks()
        bindDatabaseUpgrades()
    }

    private fun bindEbeanHooks() {
        val ebeanHooks = listOf(
                CreateUsers::class.java
        )
        for (ebeanHook in ebeanHooks) {
            bindAsContract(ebeanHook)
                    .to(EbeanHook::class.java)
                    .`in`(Singleton::class.java)
        }
    }

    private fun bindDatabaseUpgrades() {
        // NEVER EVER change order or insert elements anywhere but at the end of this list!!
        // These classes will be run in order before ebean starts
        val databaseUpgrades = listOf(
                Version1::class.java,
                Version2::class.java,
                Version3::class.java,
                Version4::class.java,
                Version5::class.java
        )
        var idx = 0
        for (databaseUpgrade in databaseUpgrades) {
            bindAsContract(databaseUpgrade)
                    .to(DatabaseUpgrade::class.java)
                    .ranked(--idx)
        }
    }
}