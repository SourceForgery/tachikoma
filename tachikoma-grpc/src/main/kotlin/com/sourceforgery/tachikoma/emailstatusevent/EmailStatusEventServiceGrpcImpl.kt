package com.sourceforgery.tachikoma.emailstatusevent

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEventServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

internal class EmailStatusEventServiceGrpcImpl
@Inject
private constructor(
    private val authentication: Authentication,
    private val emailStatsEventService: EmailStatusEventService,
    private val grpcExceptionMap: GrpcExceptionMap,
    tachikomaScope: TachikomaScope
) : EmailStatusEventServiceGrpc.EmailStatusEventServiceImplBase(), CoroutineScope by tachikomaScope {

    override fun getEmailStatusEvents(request: GetEmailStatusEventsFilter, responseObserver: StreamObserver<EmailNotification>) = grpcLaunch {
        try {
            authentication.requireFrontend()
            emailStatsEventService.getEmailStatusEvents(request, responseObserver, authentication.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
