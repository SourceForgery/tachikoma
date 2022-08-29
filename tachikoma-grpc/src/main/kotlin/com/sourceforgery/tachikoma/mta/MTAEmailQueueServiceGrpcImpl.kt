package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    private val scope: TachikomaScope by instance()

    override fun getEmails(requests: Flow<MTAQueuedNotification>): Flow<EmailMessage> = flow {
        try {
            val auth = authentication()
            auth.requireBackend()
            emitAll(mtaEmailQueueService.getEmails(requests, auth.mailDomain))
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }

    override fun getEmailsWithKeepAlive(requests: Flow<MTAQueuedNotification>) = flow {
        try {
            val keepAlive = EmailMessageOrKeepAlive.newBuilder()
                .setKeepAlive(Empty.getDefaultInstance())
                .build()

            val auth = authentication()
            auth.requireBackend()
            scope.launch {
                while (true) {
                    delay(30_000L)
                    emit(keepAlive)
                }
            }
            emitAll(
                mtaEmailQueueService.getEmails(requests, auth.mailDomain)
                    .map {
                        EmailMessageOrKeepAlive.newBuilder()
                            .setEmailMessage(it)
                            .build()
                    }
            )
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
