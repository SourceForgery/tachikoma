package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.tracking.DeliveryNotificationServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.tracking.EmailNotificationOrKeepAlive
import com.sourceforgery.tachikoma.grpc.frontend.tracking.NotificationStreamParameters
import com.sourceforgery.tachikoma.withKeepAlive
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.apache.commons.lang.RandomStringUtils
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
    private val scope: TachikomaScope by instance()

    override fun notificationStream(request: NotificationStreamParameters): Flow<EmailNotification> =
        notificationStreamWithKeepAlive(request)
            .filter { it.hasEmailNotification() }
            .map { it.emailNotification }

    override fun notificationStreamWithKeepAlive(request: NotificationStreamParameters) =
        channelFlow {
            try {
                val auth = authentication()
                auth.requireFrontend()
                LOGGER.info { "Connected, user ${auth.authenticationId} getting delivery notifications with keep-alive from ${auth.mailDomain}" }
                withKeepAlive(
                    EmailNotificationOrKeepAlive.newBuilder()
                        .setKeepAlive(RandomStringUtils.randomAlphanumeric(1000))
                        .build()
                )
                deliveryNotificationService.notificationStream(
                    request = request,
                    authenticationId = auth.authenticationId,
                    mailDomain = auth.mailDomain,
                    accountId = auth.accountId
                )
                    .map {
                        EmailNotificationOrKeepAlive.newBuilder()
                            .setEmailNotification(it)
                            .build()
                    }
                    .collect {
                        send(it)
                    }
            } catch (e: Exception) {
                throw grpcExceptionMap.findAndConvertAndLog(e)
            }
        }
            .buffer(RENDEZVOUS)

    companion object {
        private val LOGGER = logger()
    }
}
