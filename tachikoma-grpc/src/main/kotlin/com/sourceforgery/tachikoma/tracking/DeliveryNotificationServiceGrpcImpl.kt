package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.logging.logger
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class DeliveryNotificationServiceGrpcImpl
@Inject
private constructor(
        private val deliveryNotificationService: DeliveryNotificationService,
        private val grpcExceptionMap: GrpcExceptionMap,
        private val authentication: Authentication
) : DeliveryNotificationServiceGrpc.DeliveryNotificationServiceImplBase() {
    override fun notificationStream(request: NotificationStreamParameters, responseObserver: StreamObserver<EmailNotification>) {
        try {
            authentication.requireFrontend()
            LOGGER.info { "Connected, getting delivery notifications from ${authentication.mailDomain}" }
            deliveryNotificationService.notificationStream(
                    responseObserver = responseObserver,
                    request = request,
                    authenticationId = authentication.authenticationId
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}