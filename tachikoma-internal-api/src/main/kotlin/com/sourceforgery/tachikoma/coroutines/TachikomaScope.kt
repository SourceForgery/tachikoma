package com.sourceforgery.tachikoma.coroutines

import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.kodein.withRequestContext
import com.sourceforgery.tachikoma.logging.InvokeCounter
import com.sourceforgery.tachikoma.logging.InvokeCounterFactory
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

interface TachikomaScope : CoroutineScope {
    fun <T> scopedFuture(block: suspend () -> T): CompletableFuture<T>
    fun scopedLaunch(
        setup: (RequestContext, InvokeCounter) -> Unit = { _, _ -> },
        block: suspend () -> Unit
    )
    fun <T> scopedAsync(
        setup: (RequestContext, InvokeCounter) -> Unit = { _, _ -> },
        block: suspend () -> T
    ): Deferred<T>
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
        val ic = invokeCounterFactory.create()
        return withRequestContext(ctx, ic)
            .async {
                try {
                    block()
                } finally {
                    ic.dump()
                }
            }
            .asCompletableFuture()
    }

    override fun scopedLaunch(
        setup: (RequestContext, InvokeCounter) -> Unit,
        block: suspend () -> Unit
    ) {
        val ctx = RequestContext.current<ServiceRequestContext>()
        val ic = invokeCounterFactory.create()
        setup(ctx, ic)
        withRequestContext(ctx, ic)
            .launch {
                block()
            }
    }

    override fun <T> scopedAsync(
        setup: (RequestContext, InvokeCounter) -> Unit,
        block: suspend () -> T
    ): Deferred<T> {
        val ctx = RequestContext.current<ServiceRequestContext>()
        val ic = invokeCounterFactory.create()
        setup(ctx, ic)
        return withRequestContext(ctx, ic)
            .async {
                block()
            }
    }

    companion object {
        private val LOGGER = logger()
    }
}
