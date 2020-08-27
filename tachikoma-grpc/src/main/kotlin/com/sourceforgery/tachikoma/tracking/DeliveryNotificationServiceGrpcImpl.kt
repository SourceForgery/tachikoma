package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class DeliveryNotificationServiceGrpcImpl(
    override val di: DI
) : DeliveryNotificationServiceGrpc.DeliveryNotificationServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val deliveryNotificationService: DeliveryNotificationService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authentication: () -> Authentication by provider()

    override fun notificationStream(request: NotificationStreamParameters, responseObserver: StreamObserver<EmailNotification>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            LOGGER.info { "Connected, user ${auth.authenticationId} getting delivery notifications from ${auth.mailDomain}" }
            deliveryNotificationService.notificationStream(
                responseObserver = responseObserver,
                request = request,
                authenticationId = auth.authenticationId,
                mailDomain = auth.mailDomain,
                accountId = auth.accountId
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
