package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.auth.InternalCreateUserServiceImpl
import com.sourceforgery.tachikoma.database.TransactionManager
import com.sourceforgery.tachikoma.database.TransactionManagerImpl
import com.sourceforgery.tachikoma.database.auth.InternalCreateUserService
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.dao.AccountDAOImpl
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAOImpl
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAOImpl
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAOImpl
import com.sourceforgery.tachikoma.database.dao.EmailSendTransactionDAO
import com.sourceforgery.tachikoma.database.dao.EmailSendTransactionDAOImpl
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAOImpl
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailAddressDAOImpl
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAOImpl
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.database.server.DBObjectMapperImpl
import com.sourceforgery.tachikoma.database.server.DataSourceProvider
import com.sourceforgery.tachikoma.database.server.EbeanServerFactory
import com.sourceforgery.tachikoma.database.server.LogEverything
import com.sourceforgery.tachikoma.database.server.PostgresqlDataSourceProvider
import com.sourceforgery.tachikoma.database.upgrades.Version1
import com.sourceforgery.tachikoma.database.upgrades.Version10
import com.sourceforgery.tachikoma.database.upgrades.Version11
import com.sourceforgery.tachikoma.database.upgrades.Version12
import com.sourceforgery.tachikoma.database.upgrades.Version2
import com.sourceforgery.tachikoma.database.upgrades.Version3
import com.sourceforgery.tachikoma.database.upgrades.Version4
import com.sourceforgery.tachikoma.database.upgrades.Version5
import com.sourceforgery.tachikoma.database.upgrades.Version6
import com.sourceforgery.tachikoma.database.upgrades.Version7
import com.sourceforgery.tachikoma.database.upgrades.Version8
import com.sourceforgery.tachikoma.database.upgrades.Version9
import com.sourceforgery.tachikoma.kodein.DatabaseSessionKodeinScope
import com.sourceforgery.tachikoma.kodein.threadLocalDatabaseSessionScope
import com.sourceforgery.tachikoma.logging.InvokeCounter
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.provider
import org.kodein.di.registerContextFinder
import org.kodein.di.scoped
import org.kodein.di.singleton

val databaseModule = DI.Module("database") {
    bind<Database>() with singleton { EbeanServerFactory(di).provide() }

    importOnce(daoModule)
    bind<InvokeCounter>() with scoped(DatabaseSessionKodeinScope).singleton { LogEverything() }
    registerContextFinder { threadLocalDatabaseSessionScope.get() ?: error("Not in Database Session scope") }
    bind<DBObjectMapper>() with singleton { DBObjectMapperImpl }
    bind<DataSourceProvider>() with singleton { PostgresqlDataSourceProvider(di) }
    bind<InternalCreateUserService>() with singleton { InternalCreateUserServiceImpl(di) }
    bind<CreateUsers>() with singleton { CreateUsers(di) }
    bind<TransactionManager>() with singleton { TransactionManagerImpl(di) }
}

private val daoModule = DI.Module("dao") {
    bind<AccountDAO>() with singleton { AccountDAOImpl(di) }
    bind<AuthenticationDAO>() with singleton { AuthenticationDAOImpl(di) }
    bind<BlockedEmailDAO>() with singleton { BlockedEmailDAOImpl(di) }
    bind<EmailDAO>() with singleton { EmailDAOImpl(di) }
    bind<EmailSendTransactionDAO>() with singleton { EmailSendTransactionDAOImpl(di) }
    bind<EmailStatusEventDAO>() with singleton { EmailStatusEventDAOImpl(di) }
    bind<IncomingEmailAddressDAO>() with singleton { IncomingEmailAddressDAOImpl(di) }
    bind<IncomingEmailDAO>() with singleton { IncomingEmailDAOImpl(di) }
}

val databaseUpgradesModule = DI.Module("databaseUpgrades") {
    // NEVER EVER change order or insert elements anywhere but at the end of this list!!
    // These classes will be run in order before ebean starts
    bind<Version1>() with provider { Version1() }
    bind<Version2>() with provider { Version2() }
    bind<Version3>() with provider { Version3() }
    bind<Version4>() with provider { Version4() }
    bind<Version5>() with provider { Version5() }
    bind<Version6>() with provider { Version6() }
    bind<Version7>() with provider { Version7() }
    bind<Version8>() with provider { Version8() }
    bind<Version9>() with provider { Version9() }
    bind<Version10>() with provider { Version10() }
    bind<Version11>() with provider { Version11() }
    bind<Version12>() with singleton { Version12(di) }
}
