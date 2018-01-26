package com.sourceforgery.tachikoma.emailstatusevent

import com.google.protobuf.Timestamp
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.emailstatusevent.EmailStatusEvent
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import io.grpc.stub.StreamObserver
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

internal class EmailStatusEventService
@Inject
private constructor(
        private val authentication: Authentication,
        private val emailStatusEventDAO: EmailStatusEventDAO
) {
    fun getEmailStatusEvents(responseObserver: StreamObserver<EmailStatusEvent>) {

        authentication.requireFrontend()

        // TODO What is an reasonable amount of time?
        val timeFromNow = Instant.now().minus(7, ChronoUnit.DAYS)

        emailStatusEventDAO.getEventsAfter(timeFromNow)
                .forEach {
                    val dateCreated = Timestamp
                            .newBuilder()
                            .setSeconds(it.dateCreated!!.epochSecond)
                            .build()
                    responseObserver.onNext(EmailStatusEvent
                            .newBuilder()
                            .setEmailId(it.email.id.toGrpcInternal())
                            .setEmailStatus(it.emailStatus.toGrpc())
                            .setMtaStatusCode(it.mtaStatusCode)
                            .setFromEmail(it.email.transaction.fromEmail.toGrpcInternal())
                            .setRecipientEmail(it.email.recipient.toGrpcInternal())
                            .setDateCreated(dateCreated)
                            .build()
                    )
                }

        responseObserver.onCompleted()
    }
}