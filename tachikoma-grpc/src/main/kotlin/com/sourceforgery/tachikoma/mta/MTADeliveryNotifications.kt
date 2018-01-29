package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageDelivered
import com.sourceforgery.tachikoma.mq.MessageSoftBounced
import java.time.Clock
import javax.inject.Inject

internal class MTADeliveryNotifications
@Inject
private constructor(
        private val emailDAO: EmailDAO,
        private val emailStatusEventDAO: EmailStatusEventDAO,
        private val mqSender: MQSender,
        private val clock: Clock
) {
    fun setDeliveryStatus(request: DeliveryNotification) {
        val queueId = request.queueId
        val email = emailDAO.getByQueueId(queueId)
        if (email != null) {
            val creationTimestamp = clock.instant()!!
            val notificationMessageBuilder = DeliveryNotificationMessage
                    .newBuilder()
                    .setCreationTimestamp(creationTimestamp.toTimestamp())
                    .setEmailMessageId(email.id.emailId)

            val status = when (request.status) {
                "4.4.1" -> {
                    notificationMessageBuilder.messageSoftBounced = MessageSoftBounced.getDefaultInstance()
                    EmailStatus.SOFT_BOUNCED
                }
                "2.0.0" -> {
                    notificationMessageBuilder.messageDelivered = MessageDelivered.getDefaultInstance()
                    EmailStatus.DELIVERED
                }
                else -> null
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

                mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())
            } else {
                LOGGER.error { "Don't know status code ${request.status}" }
            }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}