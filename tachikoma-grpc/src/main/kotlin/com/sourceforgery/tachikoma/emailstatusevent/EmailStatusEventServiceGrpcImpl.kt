package com.sourceforgery.tachikoma.emailstatusevent

import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEventServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class EmailStatusEventServiceGrpcImpl
@Inject
private constructor(
        private val emailStatsEventService: EmailStatusEventService,
        private val grpcExceptionMap: GrpcExceptionMap
) : EmailStatusEventServiceGrpc.EmailStatusEventServiceImplBase() {

    override fun getEmailStatusEvents(request: GetEmailStatusEventsFilter, responseObserver: StreamObserver<EmailNotification>) {
        try {
            emailStatsEventService.getEmailStatusEvents(request, responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}
