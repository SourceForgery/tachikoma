package com.sourceforgery.tachikoma.database

import io.ebean.Database
import io.ebeaninternal.api.SpiEbeanServer
import io.ebeaninternal.api.SpiTransaction
import io.ebeaninternal.server.transaction.TransactionScopeManager
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
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

    override suspend fun <T> coroutineTx(block: suspend () -> T): T {
        val txManager = (database as SpiEbeanServer).transactionManager.scope()
        return database.beginTransaction().use { tx ->
            withContext(CoroutineTransactionScopeManager(txManager)) {
                block().also {
                    tx.commit()
                }
            }
        }
    }
}

internal data class TransactionScopeManagerKey(private val scopeManager: TransactionScopeManager) : CoroutineContext.Key<CoroutineTransactionScopeManager>

private class CoroutineTransactionScopeManager(
    private val transactionScopeManager: TransactionScopeManager,
    private val spiTransaction: SpiTransaction = transactionScopeManager.active
) : ThreadContextElement<SpiTransaction> {

    override val key: CoroutineContext.Key<*> = TransactionScopeManagerKey(transactionScopeManager)

    override fun restoreThreadContext(context: CoroutineContext, oldState: SpiTransaction) {
        transactionScopeManager.set(oldState)
    }

    override fun updateThreadContext(context: CoroutineContext): SpiTransaction {
        val oldState = transactionScopeManager.inScope
        transactionScopeManager.set(spiTransaction)
        return oldState
    }

    // this method is overridden to perform value comparison (==) on key
    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return if (this.key == key) EmptyCoroutineContext else this
    }

    // this method is overridden to perform value comparison (==) on key
    override operator fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? =
        @Suppress("UNCHECKED_CAST")
        if (this.key == key) this as E else null

    override fun toString(): String = "TransactionManager(value=$spiTransaction, scopeManager: $transactionScopeManager)"
}
