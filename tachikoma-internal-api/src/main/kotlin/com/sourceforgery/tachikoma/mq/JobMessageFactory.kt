package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.time.Clock
import java.time.Instant
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class JobMessageFactory(override val di: DI) : DIAware {
    private val clock: Clock by instance()

    // Factory to get all the properties set in new jobs
    fun createSendEmailJob(
        requestedSendTime: Instant = Instant.EPOCH,
        emailId: EmailId,
        mailDomain: MailDomain
    ) = JobMessage.newBuilder()
        .setCreationTimestamp(clock.instant().toTimestamp())
        .setRequestedExecutionTime(requestedSendTime.toTimestamp())
        .setSendEmailJob(SendEmailJob.newBuilder()
            .setMailDomain(mailDomain.mailDomain)
            .setEmailId(emailId.emailId)
            .build()
        )
        .build()
}
