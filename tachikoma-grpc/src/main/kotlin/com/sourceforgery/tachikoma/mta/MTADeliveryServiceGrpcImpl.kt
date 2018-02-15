package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTADeliveryServiceGrpcImpl
@Inject
private constructor(
        private val authentication: Authentication,
        private val mtaDeliveryNotifications: MTADeliveryNotifications,
        private val grpcExceptionMap: GrpcExceptionMap
) : MTADeliveryNotificationsGrpc.MTADeliveryNotificationsImplBase() {
    override fun setDeliveryStatus(request: DeliveryNotification, responseObserver: StreamObserver<Empty>) {
        return try {
            authentication.requireBackend()
            mtaDeliveryNotifications.setDeliveryStatus(request)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}