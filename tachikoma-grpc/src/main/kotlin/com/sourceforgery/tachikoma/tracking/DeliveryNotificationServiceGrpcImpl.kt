package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.EmailNotification
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class DeliveryNotificationServiceGrpcImpl
@Inject
private constructor(
        private val deliveryNotificationService: DeliveryNotificationService,
        private val grpcExceptionMap: GrpcExceptionMap
) : DeliveryNotificationServiceGrpc.DeliveryNotificationServiceImplBase() {
    override fun notificationStream(request: Empty?, responseObserver: StreamObserver<EmailNotification>) {
        try {
            deliveryNotificationService.notificationStream(responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}