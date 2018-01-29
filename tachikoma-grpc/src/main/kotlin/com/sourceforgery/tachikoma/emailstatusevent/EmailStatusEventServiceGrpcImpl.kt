package com.sourceforgery.tachikoma.emailstatusevent

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEventServiceGrpc
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class EmailStatusEventServiceGrpcImpl
@Inject
private constructor(
        private val emailStatsEventService: EmailStatusEventService,
        private val grpcExceptionMap: GrpcExceptionMap
) : EmailStatusEventServiceGrpc.EmailStatusEventServiceImplBase() {

    override fun getEmailStatusEvents(request: Empty, responseObserver: StreamObserver<EmailNotification>) {
        try {
            emailStatsEventService.getEmailStatusEvents(responseObserver)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}
