package com.sourceforgery.tachikoma.database

import io.ebean.Transaction

interface TransactionManager {
    /**
     * Run in transaction,
     * discarding the transaction if an exception is thrown from the block
     */
    fun <T> runInTransaction(block: (Transaction) -> T): T

    /**
     * Run in transaction,
     * discarding the transaction if an exception is thrown from the block
     */
    suspend fun <T> coroutineTx(block: suspend (Transaction) -> T): T
}
