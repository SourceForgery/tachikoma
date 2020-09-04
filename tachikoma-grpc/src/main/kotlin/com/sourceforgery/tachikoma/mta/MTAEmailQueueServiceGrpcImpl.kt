package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

    override fun getEmails(requests: Flow<MTAQueuedNotification>): Flow<EmailMessage> = flow {
        try {
            val auth = authentication()
            auth.requireBackend()
            emitAll(mtaEmailQueueService.getEmails(requests, auth.mailDomain))
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
