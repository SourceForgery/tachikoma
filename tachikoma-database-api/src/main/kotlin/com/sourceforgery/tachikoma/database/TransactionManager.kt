package com.sourceforgery.tachikoma.database

interface TransactionManager {
    /**
     * Run in transaction,
     * discarding the transaction if an exception is thrown from the block
     */
    fun <T> runInTransaction(block: () -> T): T

    /**
     * Run in transaction,
     * discarding the transaction if an exception is thrown from the block
     */
    suspend fun <T> coroutineTx(block: suspend () -> T): T
}
