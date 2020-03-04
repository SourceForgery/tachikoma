package com.sourceforgery.tachikoma.tracking

import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
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
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

internal class DeliveryNotificationService
@Inject
private constructor(
    private val mqSequenceFactory: MQSequenceFactory,
    private val emailDAO: EmailDAO,
    private val grpcExceptionMap: GrpcExceptionMap
) {
    fun notificationStream(
        responseObserver: StreamObserver<EmailNotification>,
        request: NotificationStreamParameters,
        authenticationId: AuthenticationId,
        accountId: AccountId,
        mailDomain: MailDomain
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
            mailDomain = mailDomain,
            accountId = accountId,
            callback = deliveryNotificationCallback
        )
        serverCallStreamObserver
            ?.setOnCancelHandler {
                future.cancel(true)
            }
        future.addListener(
            runnable(serverCallStreamObserver, future, responseObserver),
            responseCloser
        )
    }

    private fun runnable(serverCallStreamObserver: ServerCallStreamObserver<*>?, future: ListenableFuture<Void>, responseObserver: StreamObserver<*>): Runnable {
        return Runnable {
            val cancelled = serverCallStreamObserver?.isCancelled ?: true
            if (!cancelled) {
                try {
                    future.get()
                    responseObserver.onCompleted()
                } catch (e: ExecutionException) {
                    responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
                }
            }
        }
    }

    companion object {
        private val LOGGER = logger()
        private val responseCloser = Executors.newCachedThreadPool()
    }
}

private fun DeliveryNotificationMessage.toEmailNotification(emailData: EmailDBO, request: NotificationStreamParameters): EmailNotification? {
    return EmailNotification.newBuilder().apply {
        emailId = emailData.id.toGrpcInternal()
        recipientEmailAddress = emailData.recipient.toGrpcInternal()
        senderEmailAddress = emailData.transaction.fromEmail.toGrpcInternal()
        emailTransactionId = emailData.transaction.id.toGrpcInternal()
        timestamp = this@toEmailNotification.creationTimestamp
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
        if (request.includeSubject) {
            emailData.subject?.also {
                subject = it
            }
        }
        setEventData(this@toEmailNotification)
    }.build()
}

private fun EmailNotification.Builder.setEventData(deliveryNotificationMessage: DeliveryNotificationMessage): Any {
    @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
    return when (deliveryNotificationMessage.notificationDataCase) {
        DeliveryNotificationMessage.NotificationDataCase.MESSAGE_CLICKED -> {
            clickedEvent = ClickedEvent.newBuilder()
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
        DeliveryNotificationMessage.NotificationDataCase.NOTIFICATIONDATA_NOT_SET -> {
            // Skipping obviously bad message
            throw RuntimeException("Message without event. Just wrong.")
        }
    }
}
