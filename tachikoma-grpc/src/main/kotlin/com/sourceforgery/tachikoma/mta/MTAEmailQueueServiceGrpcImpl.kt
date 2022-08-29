package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.withKeepAlive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class MTAEmailQueueServiceGrpcImpl(
    override val di: DI
) : MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineImplBase(), DIAware {

    private val authentication: () -> Authentication by provider()
    private val mtaEmailQueueService: MTAEmailQueueService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getEmails(requests: Flow<MTAQueuedNotification>): Flow<EmailMessage> =
        getEmailsWithKeepAlive(requests)
            .filter { it.hasEmailMessage() }
            .map { it.emailMessage }

    override fun getEmailsWithKeepAlive(requests: Flow<MTAQueuedNotification>) = channelFlow {
        try {
            val auth = authentication()
            auth.requireBackend()
            withKeepAlive(
                EmailMessageOrKeepAlive.newBuilder()
                    .setKeepAlive(Empty.getDefaultInstance())
                    .build()
            )
            mtaEmailQueueService.getEmails(requests, auth.mailDomain)
                .map {
                    EmailMessageOrKeepAlive.newBuilder()
                        .setEmailMessage(it)
                        .build()
                }
                .collect {
                    send(it)
                }
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }

    override suspend fun incomingEmail(request: IncomingEmailMessage): MailAcceptanceResult =
        try {
            authentication().requireBackend()
            MailAcceptanceResult.newBuilder()
                .setAcceptanceStatus(mtaEmailQueueService.incomingEmail(request))
                .build()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
}
