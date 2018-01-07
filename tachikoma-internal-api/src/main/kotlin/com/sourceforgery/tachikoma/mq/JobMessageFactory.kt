package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId
import org.glassfish.hk2.api.ServiceLocator
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class JobMessageFactory
@Inject
private constructor(
        private val clock: Clock
) {

    // Factory to get all the properties set in new jobs
    fun createSendEmailJob(
            requestedSendTime: Instant = Instant.EPOCH,
            sentMailMessageBodyId: SentMailMessageBodyId,
            emailIds: Collection<EmailId>
    ) = JobMessage.newBuilder()
            .setCreationTimestamp(clock.instant().toTimestamp())
            .setRequestedExecutionTime(requestedSendTime.toTimestamp())
            .setSendEmailJob(SendEmailJob.newBuilder()
                    .addAllEmailId(emailIds.map { it.emailId })
                    .setSentMailMessageBodyId(sentMailMessageBodyId.sentMailMessageBodyId)
                    .build()
            )
            .build()
}