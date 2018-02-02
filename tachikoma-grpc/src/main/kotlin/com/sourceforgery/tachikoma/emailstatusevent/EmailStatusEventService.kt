package com.sourceforgery.tachikoma.emailstatusevent

import com.sourceforgery.tachikoma.auth.Authentication
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
import com.sourceforgery.tachikoma.grpc.frontend.SoftBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.SpamEvent
import com.sourceforgery.tachikoma.grpc.frontend.UnsubscribedEvent
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.Event
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class EmailStatusEventService
@Inject
private constructor(
        private val authentication: Authentication,
        private val authenticationDAO: AuthenticationDAO,
        private val emailStatusEventDAO: EmailStatusEventDAO
) {
    fun getEmailStatusEvents(request: GetEmailStatusEventsFilter, responseObserver: StreamObserver<EmailNotification>) {

        authentication.requireFrontend()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        val events: List<EmailStatus> = request.eventsList
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

        emailStatusEventDAO.getEvents(
                accountId = authenticationDBO.account.id,
                instant = request.newerThan?.toInstant(),
                recipientEmail = request.recipientEmail?.toEmail(),
                fromEmail = request.fromEmail?.toEmail(),
                events = events
        )
                .forEach {
                    responseObserver.onNext(
                            getEmailNotification(it)
                    )
                }
    }

    private fun getEmailNotification(it: EmailStatusEventDBO): EmailNotification {
        val builder = EmailNotification.newBuilder()
        builder.emailId = it.email.id.toGrpcInternal()
        builder.recipientEmailAddress = it.email.recipient.toGrpcInternal()
        builder.emailTransactionId = it.email.transaction.id.toGrpcInternal()
        builder.timestamp = it.dateCreated!!.toTimestamp()
        return when (it.emailStatus) {
            EmailStatus.OPENED -> {
                val ipAddress = it.metaData.ipAddress ?: ""
                builder.setOpenedEvent(OpenedEvent.newBuilder().setIpAddress(ipAddress).build())
            }

            EmailStatus.CLICKED -> {
                val ipAddress = it.metaData.ipAddress ?: ""
                builder.setClickedEvent(ClickedEvent.newBuilder().setIpAddress(ipAddress).build())
            }
            EmailStatus.HARD_BOUNCED -> {
                builder.setHardBouncedEvent(HardBouncedEvent.getDefaultInstance())
            }
            EmailStatus.DELIVERED -> {
                builder.setDelivereddEvent(DeliveredEvent.getDefaultInstance())
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