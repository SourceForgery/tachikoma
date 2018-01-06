package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.identifiers.EmailId
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class JobFactory
@Inject
private constructor(
        private val clock: Clock
) {

    // Factory to get all the properties set in new jobs
    fun createSendEmailJob(requestedSendTime: Instant = Instant.EPOCH, emailId: EmailId) =
            JobMessage.newBuilder()
                    .setCreationTimestamp(clock.instant().toTimestamp())
                    .setRequestedExecutionTime(requestedSendTime.toTimestamp())
                    .setSendEmailJob(SendEmailJob.newBuilder().addEmailId(emailId.emailId).build())
                    .build()
}