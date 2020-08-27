package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.NullStreamObserver
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.grpcFuture
import com.sourceforgery.tachikoma.grpc.grpcFutureBidi
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class MTAEmailQueueServiceGrpcImpl(
    override val di: DI
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val authentication: () -> Authentication by provider()
    private val mtaEmailQueueService: MTAEmailQueueService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getEmails(responseObserver: StreamObserver<EmailMessage>) = grpcFutureBidi(responseObserver) {
        try {
            val auth = authentication()
            auth.requireBackend()
            mtaEmailQueueService.getEmails(responseObserver, auth.mailDomain)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
            NullStreamObserver<MTAQueuedNotification>()
        }
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<MailAcceptanceResult>) = grpcFuture(responseObserver) {
        try {
            authentication().requireBackend()
            val acceptanceResult = mtaEmailQueueService.incomingEmail(request)
            responseObserver.onNext(MailAcceptanceResult.newBuilder().setAcceptanceStatus(acceptanceResult).build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
