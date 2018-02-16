package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
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
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.util.concurrent.Executors
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
        val serverCallStreamObserver = responseObserver as? ServerCallStreamObserver
        val deliveryNotificationCallback = { deliveryNotificationMessage: DeliveryNotificationMessage ->
            val emailData = emailDAO.fetchEmailData(emailMessageId = EmailId(deliveryNotificationMessage.emailMessageId))
            if (emailData == null) {
                LOGGER.error("Got event with non-existing email " + deliveryNotificationMessage.emailMessageId)
            } else {
                val emailNotification = deliveryNotificationMessage.toEmailNotification(emailData, request)
                responseObserver.onNext(emailNotification)
            }
        }
        val future = mqSequenceFactory.listenForDeliveryNotifications(
                authenticationId = authenticationId,
                callback = deliveryNotificationCallback
        )
        future.addListener(
                Runnable {
                    val cancelled = serverCallStreamObserver?.isCancelled ?: true
                    if (!cancelled) {
                        responseObserver.onCompleted()
                    }
                },
                responseCloser
        )
    }

    companion object {
        private val LOGGER = logger()
        private val responseCloser = Executors.newCachedThreadPool()!!
    }
}

private fun DeliveryNotificationMessage.toEmailNotification(emailData: EmailDBO, request: NotificationStreamParameters): EmailNotification? {
    val notificationBuilder = EmailNotification.newBuilder()
    notificationBuilder.emailId = emailData.id.toGrpcInternal()
    notificationBuilder.recipientEmailAddress = emailData.recipient.toGrpcInternal()
    notificationBuilder.emailTransactionId = emailData.transaction.id.toGrpcInternal()
    notificationBuilder.timestamp = this.creationTimestamp
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
    notificationBuilder.setEventData(this)
    return notificationBuilder
            .build()
}

private fun EmailNotification.Builder.setEventData(deliveryNotificationMessage: DeliveryNotificationMessage): Any {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (deliveryNotificationMessage.notificationDataCase) {
        DeliveryNotificationMessage.NotificationDataCase.MESSAGECLICKED -> {
            clickedEvent = ClickedEvent.newBuilder()
                    .setIpAddress(deliveryNotificationMessage.messageClicked.ipAddress)
                    .setClickedUrl(deliveryNotificationMessage.messageClicked.clickedUrl)
                    .build()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGEHARDBOUNCED -> {
            hardBouncedEvent = HardBouncedEvent.getDefaultInstance()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGEOPENED -> {
            openedEvent = OpenedEvent.newBuilder().setIpAddress(deliveryNotificationMessage.messageOpened.ipAddress).build()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGEDELIVERED -> {
            delivereddEvent = DeliveredEvent.getDefaultInstance()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGESOFTBOUNCED -> {
            softBouncedEvent = SoftBouncedEvent.getDefaultInstance()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGEUNSUBSCRIBED -> {
            unsubscribedEvent = UnsubscribedEvent.getDefaultInstance()
        }
        DeliveryNotificationMessage.NotificationDataCase.MESSAGEQUEUED -> {
            queuedEvent = QueuedEvent.getDefaultInstance()
        }
        DeliveryNotificationMessage.NotificationDataCase.NOTIFICATIONDATA_NOT_SET -> {
            // Skipping obviously bad message
            throw RuntimeException("Message without event. Just wrong.")
        }
    }
}
