package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.tracking.ClickedEvent
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveredEvent
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.HardBouncedEvent
import com.sourceforgery.tachikoma.grpc.frontend.tracking.OpenedEvent
import com.sourceforgery.tachikoma.grpc.frontend.tracking.SoftBouncedEvent
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.UserId
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
) : DeliveryNotificationServiceGrpc.DeliveryNotificationServiceImplBase() {
    override fun notificationStream(request: Empty?, responseObserver: StreamObserver<EmailNotification>) {
        mqSequenceFactory.listenForDeliveryNotifications(UserId(100), {
            val emailData = emailDAO.fetchEmailData(emailMessageId = EmailId(it.emailMessageId))
            if (emailData == null) {
                LOGGER.error("Got event with non-existing email " + it.emailMessageId)
            } else {
                val notificationBuilder = EmailNotification.newBuilder()
                notificationBuilder.emailId = emailData.id.toGrpcInternal()
                notificationBuilder.recipientEmailAddress = emailData.recipient.toGrpcInternal()
                notificationBuilder.emailTransactionId = emailData.transaction.id.toGrpcInternal()
                notificationBuilder.timestamp = it.creationTimestamp
                @Suppress("UNUSED_VARIABLE")
                val ignored = when (it.notificationDataCase!!) {
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGECLICKED -> {
                        notificationBuilder.clickedEvent = ClickedEvent.newBuilder().setIpAddress(it.messageClicked.ipAdress).build()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEHARDBOUNCED -> {
                        notificationBuilder.hardBouncedEvent = HardBouncedEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEOPENED -> {
                        notificationBuilder.openedEvent = OpenedEvent.newBuilder().setIpAddress(it.messageOpened.ipAdress).build()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGEDELIVERED -> {
                        notificationBuilder.delivereddEvent = DeliveredEvent.getDefaultInstance()
                    }
                    DeliveryNotificationMessage.NotificationDataCase.MESSAGESOFTBOUNCED -> {
                        notificationBuilder.softBouncedEvent = SoftBouncedEvent.getDefaultInstance()
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
