package com.sourceforgery.tachikoma.mq.jobs

import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.JobMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.instance

class SendEmailJob(override val di: DI) : Job {
    private val mqSender: MQSender by instance()

    override suspend fun execute(jobMessage: JobMessage) {
        val sendEmailJob = jobMessage.sendEmailJob
        val outgoingEmail = OutgoingEmailMessage.newBuilder()
            .setEmailId(sendEmailJob.emailId)
            .setCreationTimestamp(jobMessage.creationTimestamp)
            .build()
        mqSender.queueOutgoingEmail(MailDomain(sendEmailJob.mailDomain), outgoingEmail)
        LOGGER.info { "Email with id ${jobMessage.sendEmailJob.emailId} is about to be put into outgoing queue" }
    }

    companion object {
        private val LOGGER = logger()
    }
}
