package com.sourceforgery.tachikoma.mq.jobs

import com.sourceforgery.tachikoma.mq.JobMessage
import org.kodein.di.DI
import org.kodein.di.DIAware

class JobFactory(override val di: DI) : DIAware {
    fun getJobClass(jobMessage: JobMessage): Job {
        val jobDataCase = jobMessage.jobDataCase
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        return when (jobDataCase) {
            JobMessage.JobDataCase.JOBDATA_NOT_SET -> throw IllegalArgumentException("Jobdata not set")
            JobMessage.JobDataCase.SEND_EMAIL_JOB -> SendEmailJob(di)
        }
    }
}

interface Job : DIAware {
    fun execute(jobMessage: JobMessage)
}
