package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.sourceforgery.tachikoma.kodein.withInvokeCounter
import com.sourceforgery.tachikoma.logging.InvokeCounterFactory
import com.sourceforgery.tachikoma.mq.jobs.JobFactory
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class JobWorker(override val di: DI) : DIAware {
    private val mqSequenceFactory: MQSequenceFactory by instance()
    private val jobFactory: JobFactory by instance()
    private val invokeCounterFactory: InvokeCounterFactory by instance()
    private var future: ListenableFuture<Void>? = null

    fun work() {
        future = mqSequenceFactory.listenForJobs {
            withInvokeCounter(invokeCounterFactory.create()) {
                val job = jobFactory.getJobClass(it)
                job.execute(it)
            }
        }
    }
}
