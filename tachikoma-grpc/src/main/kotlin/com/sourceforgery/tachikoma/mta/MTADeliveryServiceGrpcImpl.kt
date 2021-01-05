package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class MTADeliveryServiceGrpcImpl(
    override val di: DI
) : MTADeliveryNotificationsGrpcKt.MTADeliveryNotificationsCoroutineImplBase(), DIAware {

    private val authentication: () -> Authentication by provider()
    private val mtaDeliveryNotifications: MTADeliveryNotifications by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override suspend fun setDeliveryStatus(request: DeliveryNotification): Empty =
        try {
            authentication().requireBackend()
            mtaDeliveryNotifications.setDeliveryStatus(request)
            Empty.getDefaultInstance()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
}
