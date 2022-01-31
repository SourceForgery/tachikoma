package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.common.Email
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
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.time.Clock

internal class MTADeliveryNotifications(override val di: DI) : DIAware {
    private val emailDAO: EmailDAO by instance()
    private val emailStatusEventDAO: EmailStatusEventDAO by instance()
    private val mqSender: MQSender by instance()
    private val clock: Clock by instance()

    fun setDeliveryStatus(request: DeliveryNotification) {
        LOGGER.trace { "$request" }
        val queueId = request.queueId
        val recipient = Email(request.originalRecipient)
        val email = emailDAO.getByQueueId(queueId, recipient)
        if (email == null) {
            LOGGER.warn { "Did not find any email with mtaQueueId: $queueId and associated email $recipient" }
        } else if (email.recipient == recipient) {
            val creationTimestamp = clock.instant()!!
            val notificationMessageBuilder = DeliveryNotificationMessage
                .newBuilder()
                .setCreationTimestamp(creationTimestamp.toTimestamp())
                .setEmailMessageId(email.id.emailId)

            val status = when (request.status.substring(0, 2)) {
                "2." -> {
                    if (!listOf("2.0.0", "2.6.0").contains(request.status)) {
                        LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, but we set it DELIVERED anyway" }
                    }
                    notificationMessageBuilder.messageDelivered = MessageDelivered.getDefaultInstance()
                    EmailStatus.DELIVERED
                }
                "4." -> {
                    if (!softBounceList.contains(request.status)) {
                        LOGGER.error { "Don't know status code ${request.status} for email with id ${email.id}, but we set it SOFT_BOUNCED anyway" }
                    }
                    notificationMessageBuilder.messageSoftBounced = MessageSoftBounced.getDefaultInstance()
                    EmailStatus.SOFT_BOUNCED
                }
                "5." -> {
                    if (!hardBounceList.contains(request.status)) {
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
                        mtaStatusCode = request.status,
                        mtaDiagnosticText = request.diagnoseText
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
        private val hardBounceList = listOf(
            "5.0.0", // Generic error
            "5.4.1", // Spam
            "5.5.0", // Unknown recipient
            "5.4.4", // Unable to route??
            "5.2.2", // Mailbox full
            "5.1.1", // Unknown recipient
            "5.7.1", // Unknown recipient
            "5.3.0" // Spam
        )
        private val softBounceList = listOf(
            "4.0.0", // Generic soft bounce
            "4.1.0", // Rate limited
            "4.2.0", // Temporarily deferred due to user complaints (SPAM?)
            "4.3.2", // Less used. System is shutting down/going offline
            "4.4.1", // Connection busy
            "4.4.2", // Network connection issue
            "4.7.1" // Temporarily deferred due to user complaints (SPAM?)
        )
        private val LOGGER = logger()
    }
}
