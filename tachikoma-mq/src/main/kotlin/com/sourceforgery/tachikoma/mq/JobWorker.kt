package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.mq.jobs.JobFactory
import java.util.concurrent.Executors
import javax.annotation.PreDestroy
import javax.inject.Inject

class JobWorker
@Inject
private constructor(
    private val mqSequenceFactory: MQSequenceFactory,
    private val jobFactory: JobFactory,
    private val hK2RequestContext: HK2RequestContext
) {
    private var future: ListenableFuture<Void>? = null

    fun work() {
        val ctx = hK2RequestContext.createInstance()
        // Destroy ctx when listener dies
        hK2RequestContext.runInScope(ctx) { serviceLocator ->
            future = mqSequenceFactory.listenForJobs {
                val jobClass = jobFactory.getJobClass(it)
                val job = serviceLocator.create(jobClass)
                try {
                    job.execute(it)
                } finally {
                    serviceLocator.preDestroy(job)
                }
            }.also {
                it.addListener(
                    Runnable {
                        hK2RequestContext.release(ctx)
                    },
                    scopeFinalizer
                )
            }
        }
    }

    @PreDestroy
    private fun preDestroy() {
        future?.cancel(false)
    }

    companion object {
        private val scopeFinalizer = Executors.newSingleThreadExecutor {
            Thread(it, "WorkerScopeFinalizer")
        }
    }
}
