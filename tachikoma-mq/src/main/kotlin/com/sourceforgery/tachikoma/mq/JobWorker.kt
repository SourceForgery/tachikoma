package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.sourceforgery.tachikoma.mq.jobs.JobFactory
import org.glassfish.hk2.api.ServiceLocator
import javax.annotation.PreDestroy
import javax.inject.Inject

class JobWorker
@Inject
private constructor(
        private val mqSequenceFactory: MQSequenceFactory,
        private val jobFactory: JobFactory,
        private val serviceLocator: ServiceLocator
) {
    private var future: ListenableFuture<Void>? = null

    fun work() {
        future = mqSequenceFactory.listenForJobs {
            val jobClass = jobFactory.getJobClass(it)
            val job = serviceLocator.create(jobClass)
            try {
                job.execute(it)
            } finally {
                serviceLocator.preDestroy(job)
            }
        }
    }

    @PreDestroy
    private fun preDestroy() {
        future?.cancel(false)
    }
}
