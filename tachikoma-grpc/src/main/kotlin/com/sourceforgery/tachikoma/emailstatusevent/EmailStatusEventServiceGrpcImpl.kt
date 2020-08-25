package com.sourceforgery.tachikoma.emailstatusevent

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEventServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class EmailStatusEventServiceGrpcImpl(
    override val di: DI
) : EmailStatusEventServiceGrpc.EmailStatusEventServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val authentication: () -> Authentication by provider()
    private val emailStatsEventService: EmailStatusEventService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getEmailStatusEvents(request: GetEmailStatusEventsFilter, responseObserver: StreamObserver<EmailNotification>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            emailStatsEventService.getEmailStatusEvents(request, responseObserver, auth.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
