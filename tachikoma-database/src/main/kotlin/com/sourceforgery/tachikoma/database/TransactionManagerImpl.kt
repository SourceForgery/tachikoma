package com.sourceforgery.tachikoma.database

import io.ebean.Database
import javax.inject.Inject

class TransactionManagerImpl
@Inject
private constructor(
    private val database: Database
) : TransactionManager {
    override fun <T> runInTransaction(block: () -> T): T {
        return database.beginTransaction().use { tx ->
            block().also {
                tx.commit()
            }
        }
    }
}