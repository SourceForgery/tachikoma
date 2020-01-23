package com.sourceforgery.tachikoma.mq.jobs

import com.sourceforgery.tachikoma.mq.JobMessage
import javax.inject.Inject

class JobFactory
@Inject
private constructor() {
    fun getJobClass(jobMessage: JobMessage): Class<out Job> {
        val jobDataCase = jobMessage.jobDataCase
        return when (jobDataCase) {
            JobMessage.JobDataCase.JOBDATA_NOT_SET -> throw IllegalArgumentException("Jobdata not set")
            JobMessage.JobDataCase.SEND_EMAIL_JOB -> SendEmailJob::class.java
        }
    }
}

interface Job {
    fun execute(jobMessage: JobMessage)
}
