package com.sourceforgery.tachikoma.emailstatusevent

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.ClickedEvent
import com.sourceforgery.tachikoma.grpc.frontend.DeliveredEvent
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.HardBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.OpenedEvent
import com.sourceforgery.tachikoma.grpc.frontend.QueuedEvent
import com.sourceforgery.tachikoma.grpc.frontend.SentEmailTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.SoftBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.SpamEvent
import com.sourceforgery.tachikoma.grpc.frontend.UnsubscribedEvent
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.Event
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.tracking.toEmailMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

internal class EmailStatusEventService(override val di: DI) : DIAware {
    private val authenticationDAO: AuthenticationDAO by instance()
    private val emailStatusEventDAO: EmailStatusEventDAO by instance()

    fun getEmailStatusEvents(
        request: GetEmailStatusEventsFilter,
        authenticationId: AuthenticationId,
    ): Flow<EmailNotification> {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        val events: List<EmailStatus> =
            request.eventsList
                .map {
                    when (it) {
                        Event.CLICKED -> EmailStatus.CLICKED
                        Event.DELIVERED -> EmailStatus.DELIVERED
                        Event.UNSUBSCRIBED -> EmailStatus.UNSUBSCRIBE
                        Event.HARD_BOUNCED -> EmailStatus.HARD_BOUNCED
                        Event.SOFT_BOUNCED -> EmailStatus.SOFT_BOUNCED
                        Event.QUEUED -> EmailStatus.QUEUED
                        Event.SPAM -> EmailStatus.SPAM
                        Event.OPENED -> EmailStatus.OPENED
                        else -> throw IllegalArgumentException("$it is not valid for blocking")
                    }
                }
        val includeTrackingData = request.includeTrackingData
        val includeMetricsData = request.includeMetricsData

        return emailStatusEventDAO.getEvents(
            accountId = authenticationDBO.account.id,
            instant = request.newerThan.toInstant(),
            recipientEmail = request.recipientEmail.toEmail(),
            fromEmail = request.fromEmail.toEmail(),
            events = events,
            tags = request.tagsList.toSet(),
        ).asFlow()
            .map { getEmailNotification(it, includeTrackingData, includeMetricsData) }
    }

    private fun getEmailNotification(
        emailStatusEventDBO: EmailStatusEventDBO,
        includeTrackingData: Boolean,
        includeMetricsData: Boolean,
    ): EmailNotification {
        val builder = EmailNotification.newBuilder()
        builder.emailId = emailStatusEventDBO.email.id.toGrpcInternal()
        builder.recipientEmailAddress = emailStatusEventDBO.email.recipient.toGrpcInternal()
        builder.senderEmailAddress = emailStatusEventDBO.email.transaction.fromEmail.toGrpcInternal()
        builder.emailTransactionId = emailStatusEventDBO.email.transaction.id.toGrpcInternal()
        builder.timestamp = emailStatusEventDBO.dateCreated!!.toTimestamp()

        if (includeMetricsData) {
            builder.emailMetrics = emailStatusEventDBO.email.toEmailMetrics()
        } else {
            builder.noMetricsData = Empty.getDefaultInstance()
        }

        if (includeTrackingData) {
            builder.setEmailTrackingData(SentEmailTrackingData.newBuilder())
            // TODO Insert logic to retrieve tracking data include it
        } else {
            builder.noTrackingData = Empty.getDefaultInstance()
        }

        return when (emailStatusEventDBO.emailStatus) {
            EmailStatus.OPENED -> {
                val ipAddress = emailStatusEventDBO.metaData.ipAddress ?: ""
                builder.setOpenedEvent(
                    OpenedEvent.newBuilder()
                        .setIpAddress(ipAddress)
                        .setUserAgent(emailStatusEventDBO.metaData.userAgent ?: "")
                        .build(),
                )
            }

            EmailStatus.CLICKED -> {
                val ipAddress = emailStatusEventDBO.metaData.ipAddress ?: ""
                builder.setClickedEvent(
                    ClickedEvent.newBuilder()
                        .setIpAddress(ipAddress)
                        .setUserAgent(emailStatusEventDBO.metaData.userAgent ?: "")
                        .build(),
                )
            }
            EmailStatus.HARD_BOUNCED -> {
                builder.setHardBouncedEvent(HardBouncedEvent.getDefaultInstance())
            }
            EmailStatus.DELIVERED -> {
                builder.setDeliveredEvent(DeliveredEvent.getDefaultInstance())
            }
            EmailStatus.SOFT_BOUNCED -> {
                builder.setSoftBouncedEvent(SoftBouncedEvent.getDefaultInstance())
            }
            EmailStatus.UNSUBSCRIBE -> {
                builder.setUnsubscribedEvent(UnsubscribedEvent.getDefaultInstance())
            }
            EmailStatus.QUEUED -> {
                builder.setQueuedEvent(QueuedEvent.getDefaultInstance())
            }
            EmailStatus.SPAM -> {
                builder.setSpamEvent(SpamEvent.getDefaultInstance())
            }
        }.build()
    }
}
