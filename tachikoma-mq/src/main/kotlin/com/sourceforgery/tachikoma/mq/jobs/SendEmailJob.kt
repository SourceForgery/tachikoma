package com.sourceforgery.tachikoma.mq.jobs

import com.sourceforgery.tachikoma.mq.JobMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import javax.inject.Inject

class SendEmailJob
@Inject
private constructor(
        private val mqSender: MQSender
) : Job {
    override fun execute(jobMessage: JobMessage) {
        val sendEmailJob = jobMessage.sendEmailJob
        val outgoingEmail = OutgoingEmailMessage.newBuilder()
                .setEmailId(sendEmailJob.emailId)
                .setCreationTimestamp(jobMessage.creationTimestamp)
                .build()
        mqSender.queueOutgoingEmail(outgoingEmail)
    }
}
