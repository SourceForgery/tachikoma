package com.sourceforgery.tachikoma.mq.jobs

import com.sourceforgery.tachikoma.mq.JobMessage
import org.glassfish.hk2.api.ServiceLocator
import javax.inject.Inject

class JobFactory
@Inject
private constructor(
        private val serviceLocator: ServiceLocator
) {
    fun getJobClass(jobMessage: JobMessage): Class<out Job> {
        val jobDataCase = jobMessage.jobDataCase
                ?: throw IllegalArgumentException("Unknown job type")
        return when (jobDataCase) {
            JobMessage.JobDataCase.JOBDATA_NOT_SET -> throw IllegalArgumentException("Jobdata not set")
            JobMessage.JobDataCase.SENDEMAILJOB -> SendEmailJob::class.java
        }
    }
}

interface Job {
    fun execute(jobMessage: JobMessage);
}
