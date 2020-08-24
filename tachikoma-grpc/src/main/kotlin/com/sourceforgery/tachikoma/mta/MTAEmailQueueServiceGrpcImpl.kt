package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.NullStreamObserver
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class MTAEmailQueueServiceGrpcImpl
@Inject
private constructor(
    private val authentication: Authentication,
    private val mtaEmailQueueService: MTAEmailQueueService,
    private val grpcExceptionMap: GrpcExceptionMap,
    tachikomaScope: TachikomaScope
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase(), CoroutineScope by tachikomaScope {
    override fun getEmails(responseObserver: StreamObserver<EmailMessage>): StreamObserver<MTAQueuedNotification> {
        return try {
            authentication.requireBackend()
            mtaEmailQueueService.getEmails(responseObserver, authentication.mailDomain)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
            NullStreamObserver()
        }
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<MailAcceptanceResult>) = grpcLaunch {
        try {
            authentication.requireBackend()
            val acceptanceResult = mtaEmailQueueService.incomingEmail(request)
            responseObserver.onNext(MailAcceptanceResult.newBuilder().setAcceptanceStatus(acceptanceResult).build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
