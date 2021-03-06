package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class DeliveryNotificationServiceGrpcImpl(
    override val di: DI
) : DeliveryNotificationServiceGrpcKt.DeliveryNotificationServiceCoroutineImplBase(), DIAware {

    private val deliveryNotificationService: DeliveryNotificationService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authentication: () -> Authentication by provider()

    override fun notificationStream(request: NotificationStreamParameters): Flow<EmailNotification> = flow {
        try {
            val auth = authentication()
            auth.requireFrontend()
            LOGGER.info { "Connected, user ${auth.authenticationId} getting delivery notifications from ${auth.mailDomain}" }
            emitAll(
                deliveryNotificationService.notificationStream(
                    request = request,
                    authenticationId = auth.authenticationId,
                    mailDomain = auth.mailDomain,
                    accountId = auth.accountId
                )
            )
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
