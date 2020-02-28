package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageDelivered
import com.sourceforgery.tachikoma.mq.MessageHardBounced
import com.sourceforgery.tachikoma.mq.MessageSoftBounced
import java.time.Clock
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

internal class MTADeliveryNotifications
@Inject
private constructor(
    private val emailDAO: EmailDAO,
    private val emailStatusEventDAO: EmailStatusEventDAO,
    private val mqSender: MQSender,
    private val clock: Clock
) {
    fun setDeliveryStatus(request: DeliveryNotification) {
        LOGGER.debug { "$request" }
        val queueId = request.queueId
        val email = emailDAO.getByQueueId(queueId)
        if (email != null) {
            val creationTimestamp = clock.instant()!!
            val notificationMessageBuilder = DeliveryNotificationMessage
                .newBuilder()
                .setCreationTimestamp(creationTimestamp.toTimestamp())
                .setEmailMessageId(email.id.emailId)

            val status = when (request.status.substring(0, 2)) {
                "2." -> {
                    if (!arrayOf("2.0.0", "2.6.0").contains(request.status)) {
                        LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, but we set it DELIVERED anyway" }
                    }
                    notificationMessageBuilder.messageDelivered = MessageDelivered.getDefaultInstance()
                    EmailStatus.DELIVERED
                }
                "4." -> {
                    if (!arrayOf("4.0.0", "4.4.1").contains(request.status)) {
                        LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, but we set it SOFT_BOUNCED anyway" }
                    }
                    notificationMessageBuilder.messageSoftBounced = MessageSoftBounced.getDefaultInstance()
                    EmailStatus.SOFT_BOUNCED
                }
                "5." -> {
                    if (!arrayOf("5.0.0").contains(request.status)) {
                        LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, but we set it HARD_BOUNCED anyway" }
                    }
                    notificationMessageBuilder.messageHardBounced = MessageHardBounced.getDefaultInstance()
                    EmailStatus.HARD_BOUNCED
                }
                else -> {
                    LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, not sending event" }
                    null
                }
            }

            if (status != null) {
                val statusEventDBO = EmailStatusEventDBO(
                    emailStatus = status,
                    email = email,
                    metaData = StatusEventMetaData(
                        mtaStatusCode = request.status
                    )
                )
                statusEventDBO.dateCreated = creationTimestamp
                emailStatusEventDAO.save(statusEventDBO)
                LOGGER.debug { "Setting status $status for email ${email.messageId}" }
                mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())
            }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}