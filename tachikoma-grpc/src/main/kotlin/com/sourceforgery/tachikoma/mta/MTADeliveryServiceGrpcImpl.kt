package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

internal class MTADeliveryServiceGrpcImpl
@Inject
private constructor(
    private val authentication: Authentication,
    private val mtaDeliveryNotifications: MTADeliveryNotifications,
    private val grpcExceptionMap: GrpcExceptionMap,
    tachikomaScope: TachikomaScope
) : MTADeliveryNotificationsGrpc.MTADeliveryNotificationsImplBase(), CoroutineScope by tachikomaScope {
    override fun setDeliveryStatus(request: DeliveryNotification, responseObserver: StreamObserver<Empty>) = grpcLaunch {
        try {
            authentication.requireBackend()
            mtaDeliveryNotifications.setDeliveryStatus(request)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
