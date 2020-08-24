package com.sourceforgery.tachikoma.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.apache.logging.log4j.kotlin.logger
import kotlin.coroutines.CoroutineContext

class TachikomaScope : CoroutineScope {
    private val job: Job = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        LOGGER.error(exception) { "Coroutine uncaught exception" }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job + exceptionHandler

    companion object {
        private val LOGGER = logger()
    }
}
