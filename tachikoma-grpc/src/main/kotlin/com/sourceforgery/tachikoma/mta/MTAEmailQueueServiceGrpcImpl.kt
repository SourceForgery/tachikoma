package com.sourceforgery.tachikoma.mta

import com.sourceforgery.tachikoma.grpc.NullStreamObserver
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.NullStreamObserver
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTAEmailQueueServiceGrpcImpl
@Inject
private constructor(
        private val mtaEmailQueueService: MTAEmailQueueService,
        private val grpcExceptionMap: GrpcExceptionMap
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun getEmails(responseObserver: StreamObserver<EmailMessage>): StreamObserver<MTAQueuedNotification> {
        return try {
            mtaEmailQueueService.getEmails(responseObserver)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
            NullStreamObserver()
        }
    }

    override fun incomingEmail(request: IncomingEmailMessage, responseObserver: StreamObserver<MailAcceptanceResult>) {
        return try {
            val acceptanceResult = mtaEmailQueueService.incomingEmail(request)
            responseObserver.onNext(MailAcceptanceResult.newBuilder().setAcceptanceStatus(acceptanceResult).build())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}