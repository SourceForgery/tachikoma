package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.Empty
import com.google.protobuf.Timestamp
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.BlockedReason
import com.sourceforgery.tachikoma.grpc.frontend.ClickedEvent
import com.sourceforgery.tachikoma.grpc.frontend.DeliveredEvent
import com.sourceforgery.tachikoma.grpc.frontend.EmailMetrics
import com.sourceforgery.tachikoma.grpc.frontend.EmailMetricsClickData
import com.sourceforgery.tachikoma.grpc.frontend.EmailMetricsOpenData
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.HardBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.MessageId
import com.sourceforgery.tachikoma.grpc.frontend.OpenedEvent
import com.sourceforgery.tachikoma.grpc.frontend.QueuedEvent
import com.sourceforgery.tachikoma.grpc.frontend.ReportedAbuseEvent
import com.sourceforgery.tachikoma.grpc.frontend.SentEmailTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.SoftBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.UnsubscribedEvent
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.tracking.EmailNotificationOrKeepAlive
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.invoke
import com.sourceforgery.tachikoma.mq.BlockedEmailAddressEvent
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.EmailNotificationEvent
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

internal class DeliveryNotificationService(override val di: DI) : DIAware {
    private val mqSequenceFactory: MQSequenceFactory by instance()
    private val emailDAO: EmailDAO by instance()

    fun notificationStream(
        request: NotificationStreamParameters,
        authenticationId: AuthenticationId,
        accountId: AccountId,
        mailDomain: MailDomain,
        includeTags: Set<String>,
    ): Flow<EmailNotificationOrKeepAlive> =
        mqSequenceFactory.listenForDeliveryAndBlockNotifications(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
        ).mapNotNull { emailNotificationEvent: EmailNotificationEvent ->
            when {
                emailNotificationEvent.hasDeliveryNotification() -> {
                    val deliveryNotificationMessage = emailNotificationEvent.deliveryNotification
                    val emailData = emailDAO.fetchEmailData(emailMessageId = EmailId(deliveryNotificationMessage.emailMessageId))
                    if (emailData == null) {
                        LOGGER.error("Got event with non-existing email " + deliveryNotificationMessage.emailMessageId)
                        return@mapNotNull null
                    }

                    if (includeTags.isNotEmpty()) {
                        if (emailData.transaction.tags.intersect(includeTags).isEmpty()) {
                            return@mapNotNull null
                        }
                    }

                    deliveryNotificationMessage.toEmailNotification(emailData, request, emailNotificationEvent.creationTimestamp)
                }

                emailNotificationEvent.hasBlockedEmailAddress() -> {
                    emailNotificationEvent.blockedEmailAddress.toEmailNotification()
                }

                else -> null
            }
        }

    companion object {
        private val LOGGER = logger()
    }
}

private fun BlockedEmailAddressEvent.toEmailNotification(): EmailNotificationOrKeepAlive {
    return (EmailNotificationOrKeepAlive.newBuilder()) {
        emailAddressNotificationBuilder {
            fromEmail = this@toEmailNotification.fromEmail
            recipientEmail = this@toEmailNotification.recipientEmail
            newReason = this@toEmailNotification.newReason.toGrpc()
        }
    }.build()
}

private fun DeliveryNotificationMessage.toEmailNotification(
    emailData: EmailDBO,
    request: NotificationStreamParameters,
    creationTimestamp: Timestamp,
): EmailNotificationOrKeepAlive {
    return (EmailNotificationOrKeepAlive.newBuilder()) {
        emailNotificationBuilder {
            emailId = emailData.id.toGrpcInternal()
            recipientEmailAddress = emailData.recipient.toGrpcInternal()
            senderEmailAddress = emailData.transaction.fromEmail.toGrpcInternal()
            emailTransactionId = emailData.transaction.id.toGrpcInternal()
            timestamp = creationTimestamp
            messageId = MessageId.newBuilder().setMessageId(emailData.messageId.messageId).build()

            if (request.includeTrackingData) {
                emailTrackingData =
                    SentEmailTrackingData.newBuilder()
                        .addAllTags(emailData.transaction.tags)
                        .putAllMetadata(emailData.transaction.metaData)
                        .putAllMetadata(emailData.metaData)
                        .build()
            } else {
                noTrackingData = Empty.getDefaultInstance()
            }

            if (request.includeMetricsData) {
                emailMetrics = emailData.toEmailMetrics()
            } else {
                noMetricsData = Empty.getDefaultInstance()
            }

            if (request.includeSubject) {
                emailData.subject?.also {
                    subject = it
                }
            }
            setEventData(this@toEmailNotification)
        }
    }.build()
}

fun EmailDBO.toEmailMetrics(): EmailMetrics =
    EmailMetrics.newBuilder()
        .addAllOpens(
            emailStatusEvents.filter { it.emailStatus == EmailStatus.OPENED }
                .map {
                    EmailMetricsOpenData.newBuilder()
                        .setIpAddress(it.metaData.ipAddress ?: "")
                        .setTimestamp(it.dateCreated!!.toTimestamp())
                        .setUserAgent(it.metaData.userAgent ?: "")
                        .build()
                },
        )
        .addAllClicks(
            emailStatusEvents.filter { it.emailStatus == EmailStatus.CLICKED }
                .map {
                    EmailMetricsClickData.newBuilder()
                        .setIpAddress(it.metaData.ipAddress ?: "")
                        .setTimestamp(it.dateCreated!!.toTimestamp())
                        .setUserAgent(it.metaData.userAgent ?: "")
                        .build()
                },
        ).build()

private fun EmailNotification.Builder.setEventData(deliveryNotificationMessage: DeliveryNotificationMessage): Any {
    return when (deliveryNotificationMessage.notificationDataCase) {
        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_CLICKED -> {
            clickedEvent =
                ClickedEvent.newBuilder()
                    .setIpAddress(deliveryNotificationMessage.messageClicked.ipAddress)
                    .setClickedUrl(deliveryNotificationMessage.messageClicked.clickedUrl)
                    .build()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_HARD_BOUNCED -> {
            hardBouncedEvent = HardBouncedEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_OPENED -> {
            openedEvent = OpenedEvent.newBuilder().setIpAddress(deliveryNotificationMessage.messageOpened.ipAddress).build()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_DELIVERED -> {
            deliveredEvent = DeliveredEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_SOFT_BOUNCED -> {
            softBouncedEvent = SoftBouncedEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_UNSUBSCRIBED -> {
            unsubscribedEvent = UnsubscribedEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_QUEUED -> {
            queuedEvent = QueuedEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_REPORTED_ABUSE -> {
            reportedAbuseEvent = ReportedAbuseEvent.getDefaultInstance()
        }

        DeliveryNotificationMessage.NotificationDataCase.NOTIFICATIONDATA_NOT_SET -> {
            // Skipping obviously bad message
            throw RuntimeException("Message without event. Just wrong.")
        }
    }
}

fun com.sourceforgery.tachikoma.mq.BlockedReason.toGrpc(): BlockedReason =
    when (this) {
        com.sourceforgery.tachikoma.mq.BlockedReason.UNSUBSCRIBED -> BlockedReason.UNSUBSCRIBED
        com.sourceforgery.tachikoma.mq.BlockedReason.SPAM_MARKED -> BlockedReason.SPAM_MARKED
        com.sourceforgery.tachikoma.mq.BlockedReason.HARD_BOUNCED -> BlockedReason.HARD_BOUNCED
        com.sourceforgery.tachikoma.mq.BlockedReason.UNRECOGNIZED -> BlockedReason.UNRECOGNIZED
        com.sourceforgery.tachikoma.mq.BlockedReason.UNBLOCKED -> BlockedReason.UNBLOCKED
    }
