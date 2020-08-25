package com.sourceforgery.tachikoma.coroutines

import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import org.apache.logging.log4j.kotlin.logger
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

interface TachikomaScope : CoroutineScope{
    fun <T> future(block: suspend (RequestContext) -> T): CompletableFuture<T>
}

class TachikomaScopeImpl
@Inject
constructor(
    private val hK2RequestContext: HK2RequestContext
) : CoroutineScope, TachikomaScope {
    private val job: Job = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        LOGGER.error(exception) { "Coroutine uncaught exception" }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + exceptionHandler


    override fun <T> future(block: suspend (RequestContext) -> T): CompletableFuture<T> {
        val ctx = RequestContext.current<ServiceRequestContext>()
        return async(hK2RequestContext.asContextElement()) {
            block(ctx)
        }.asCompletableFuture()
    }


    companion object {
        private val LOGGER = logger()
    }
}
