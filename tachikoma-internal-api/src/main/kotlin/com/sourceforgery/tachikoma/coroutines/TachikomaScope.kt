package com.sourceforgery.tachikoma.coroutines

import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.kodein.DatabaseSessionContext
import com.sourceforgery.tachikoma.kodein.DatabaseSessionKodeinScope
import com.sourceforgery.tachikoma.kodein.withRequestContext
import com.sourceforgery.tachikoma.logging.InvokeCounterFactory
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

interface TachikomaScope : CoroutineScope {
    fun <T> scopedFuture(block: suspend () -> T): CompletableFuture<T>
}

class TachikomaScopeImpl(override val di: DI) : CoroutineScope, TachikomaScope, DIAware {
    private val job: Job = SupervisorJob()
    private val invokeCounterFactory: InvokeCounterFactory by instance()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        LOGGER.error(exception) { "Coroutine uncaught exception" }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + exceptionHandler

    override fun <T> scopedFuture(block: suspend () -> T): CompletableFuture<T> {
        val ctx = RequestContext.current<ServiceRequestContext>()
        val databaseSessionContext = DatabaseSessionContext()
        return withRequestContext(ctx, databaseSessionContext)
            .async {
                try {
                    block()
                } finally {
                    DatabaseSessionKodeinScope.getRegistry(databaseSessionContext).close()
                }
            }
            .asCompletableFuture()
    }

    companion object {
        private val LOGGER = logger()
    }
}
