package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class MTADeliveryServiceGrpcImpl(
    override val di: DI
) : MTADeliveryNotificationsGrpc.MTADeliveryNotificationsImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val authentication: () -> Authentication by provider()
    private val mtaDeliveryNotifications: MTADeliveryNotifications by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun setDeliveryStatus(request: DeliveryNotification, responseObserver: StreamObserver<Empty>) = grpcFuture(responseObserver) {
        try {
            authentication().requireBackend()
            mtaDeliveryNotifications.setDeliveryStatus(request)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
