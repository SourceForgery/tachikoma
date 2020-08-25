package com.sourceforgery.tachikoma.database

import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class TransactionManagerImpl(override val di: DI) : TransactionManager, DIAware {
    private val database: Database by instance()
    override fun <T> runInTransaction(block: () -> T): T {
        return database.beginTransaction().use { tx ->
            block().also {
                tx.commit()
            }
        }
    }
}
