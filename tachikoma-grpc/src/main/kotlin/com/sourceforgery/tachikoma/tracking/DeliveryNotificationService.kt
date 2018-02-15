package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.ClickedEvent
import com.sourceforgery.tachikoma.grpc.frontend.DeliveredEvent
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.HardBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.MessageId
import com.sourceforgery.tachikoma.grpc.frontend.OpenedEvent
import com.sourceforgery.tachikoma.grpc.frontend.QueuedEvent
import com.sourceforgery.tachikoma.grpc.frontend.SentEmailTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.SoftBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.UnsubscribedEvent
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class DeliveryNotificationService
@Inject
private constructor(
        private val mqSequenceFactory: MQSequenceFactory,
        private val emailDAO: EmailDAO
) {
    fun notificationStream(
            responseObserver: StreamObserver<EmailNotification>,
            request: NotificationStreamParameters,
            authenticationId: AuthenticationId
    ) {
        mqSequenceFactory.listenForDeliveryNotifications(authenticationId, {
            val emailData = emailDAO.fetchEmailData(emailMessageId = EmailId(it.emailMessageId))
            if (emailData == null) {
                LOGGER.error("Got event with non-existing email " + it.emailMessageId)
            } else {
                val notificationBuilder = EmailNotification.newBuilder()
                notificationBuilder.emailId = emailData.id.toGrpcInternal()
                notificationBuilder.recipientEmailAddress = emailData.recipient.toGrpcInternal()
                notificationBuilder.emailTransactionId = emailData.transaction.id.toGrpcInternal()
                notificationBuilder.timestamp = it.creationTimestamp
                notificationBuilder.messageId = MessageId.newBuilder().setMessageId(emailData.messageId.messageId).build()
                if (request.includeTrackingData) {
                    notificationBuilder.emailTrackingData =
                            SentEmailTrackingData.newBuilder()
                                    .addAllTags(emailData.transaction.tags)
                                    .putAllMetadata(emailData.transaction.metaData)
                                    .putAllMetadata(emailData.metaData)
                                    .build()
                } else {
                    notificationBuilder.setNoTrackingData(Empty.getDefaultInstance())
                }
                if (request.includeSubject) {
                    emailData.subject?.also {
                        notificationBuilder.subject = it
                    }
                }
                @Suppress("UNUSED_VARIABLE")
                val ignored = when (it.notificationDataCase) {
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGECLICKED -> {
                        notificationBuilder.clickedEvent = ClickedEvent.newBuilder()
                                .setIpAddress(it.messageClicked.ipAddress)
                                .setClickedUrl(it.messageClicked.clickedUrl)
                                .build()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEHARDBOUNCED -> {
                        notificationBuilder.hardBouncedEvent = HardBouncedEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEOPENED -> {
                        notificationBuilder.openedEvent = OpenedEvent.newBuilder().setIpAddress(it.messageOpened.ipAddress).build()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEDELIVERED -> {
                        notificationBuilder.delivereddEvent = DeliveredEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGESOFTBOUNCED -> {
                        notificationBuilder.softBouncedEvent = SoftBouncedEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEUNSUBSCRIBED -> {
                        notificationBuilder.unsubscribedEvent = UnsubscribedEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEQUEUED -> {
                        notificationBuilder.queuedEvent = QueuedEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.NOTIFICATIONDATA_NOT_SET -> {
                        // Skipping obviously bad message
                        throw RuntimeException("Message without event. Just wrong.")
                    }
                }
                responseObserver.onNext(notificationBuilder.build())
            }
        })
    }

    companion object {
        val LOGGER = logger()
    }
}
