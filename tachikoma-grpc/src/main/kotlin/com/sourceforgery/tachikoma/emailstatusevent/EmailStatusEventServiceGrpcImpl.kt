package com.sourceforgery.tachikoma.emailstatusevent

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.EmailNotification
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEventServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.GetEmailStatusEventsFilter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class EmailStatusEventServiceGrpcImpl(
    override val di: DI
) : EmailStatusEventServiceGrpcKt.EmailStatusEventServiceCoroutineImplBase(), DIAware {

private val authentication: () -> Authentication by provider()
    private val emailStatsEventService: EmailStatusEventService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getEmailStatusEvents(request: GetEmailStatusEventsFilter) = flow<EmailNotification> {
        val auth = authentication()
        auth.requireFrontend()
        emitAll(emailStatsEventService.getEmailStatusEvents(request, auth.authenticationId))
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }
}
