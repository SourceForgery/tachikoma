package com.sourceforgery.tachikoma.database

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import io.ebeaninternal.api.SpiEbeanServer
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class TransactionManagerImplTest : DIAware {

    override val di = DI {
        importOnce(testModule(), allowOverride = true)
    }

    private val database: Database by instance()
    private val transactionManager: TransactionManager by instance()

    @Test
    fun `clean transaction`() {
        val txManager = (database as SpiEbeanServer).transactionManager.scope()
        runBlocking {
            val thread = Thread.currentThread()
            lateinit var id: AccountId
            assertNull(txManager.inScope)
            transactionManager.coroutineTx { tx ->
                withContext(Dispatchers.IO) {
                    val accountDBO = AccountDBO(MailDomain("${UUID.randomUUID()}.example.com"))
                    database.save(accountDBO)
                    id = accountDBO.id
                    assertNotSame(thread, Thread.currentThread())
                    assertSame(tx, txManager.inScope)
                }
            }
            assertNull(txManager.inScope)
            val account = database.find<AccountDBO>(id)
            assertNotNull(account)
        }
    }

    @Test
    fun `aborted transaction`() {
        val txManager = (database as SpiEbeanServer).transactionManager.scope()
        runBlocking {
            val thread = Thread.currentThread()
            lateinit var id: AccountId
            assertNull(txManager.inScope)
            assertFailsWith(IllegalStateException::class) {
                transactionManager.coroutineTx { tx ->
                    withContext(Dispatchers.IO) {
                        val accountDBO = AccountDBO(MailDomain("${UUID.randomUUID()}.example.com"))
                        database.save(accountDBO)
                        id = accountDBO.id
                        assertNotSame(thread, Thread.currentThread())
                        assertSame(tx, txManager.inScope)
                        val account = database.find<AccountDBO>(id)
                        assertNotNull(account)
                        error("")
                    }
                }
            }
            assertNull(txManager.inScope)
            val account = database.find<AccountDBO>(id)
            assertNull(account)
        }
    }

    @Test
    fun `aborted transaction and then create new transaction`() {
        val txManager = (database as SpiEbeanServer).transactionManager.scope()
        runBlocking {
            val thread = Thread.currentThread()
            lateinit var id: AccountId
            assertNull(txManager.inScope)
            assertFailsWith(IllegalStateException::class) {
                transactionManager.coroutineTx { tx ->
                    withContext(Dispatchers.IO) {
                        val accountDBO = AccountDBO(MailDomain("${UUID.randomUUID()}.example.com"))
                        database.save(accountDBO)
                        id = accountDBO.id
                        assertNotSame(thread, Thread.currentThread())
                        assertSame(tx, txManager.inScope)
                        val account = database.find<AccountDBO>(id)
                        assertNotNull(account)
                        error("")
                    }
                }
            }
            assertNull(txManager.inScope)
            assertNull(database.find<AccountDBO>(id))
            transactionManager.coroutineTx { tx ->
                val accountDBO = AccountDBO(MailDomain("${UUID.randomUUID()}.example.com"))
                database.save(accountDBO)
                id = accountDBO.id
            }
            assertNotNull(database.find<AccountDBO>(id))
        }
    }
}
