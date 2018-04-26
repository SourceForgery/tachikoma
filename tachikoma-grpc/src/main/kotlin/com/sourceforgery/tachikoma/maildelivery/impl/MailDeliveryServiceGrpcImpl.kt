package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.logging.logger
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MailDeliveryServiceGrpcImpl
@Inject
private constructor(
    private val mailDeliveryService: MailDeliveryService,
    private val authentication: Authentication,
    private val grpcExceptionMap: GrpcExceptionMap
) : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase() {
    override fun getIncomingEmails(request: Empty, responseObserver: StreamObserver<IncomingEmail>) {
        try {
            authentication.requireFrontend()
            LOGGER.info { "Connected, user ${authentication.authenticationId} getting incoming mails from ${authentication.mailDomain}" }
            mailDeliveryService.getIncomingEmails(
                responseObserver = responseObserver,
                authenticationId = authentication.authenticationId,
                mailDomain = authentication.mailDomain,
                accountId = authentication.accountId
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) {
        try {
            authentication.requireFrontend()
            mailDeliveryService.sendEmail(
                request = request,
                responseObserver = responseObserver,
                authenticationId = authentication.authenticationId,
                sender = authentication.accountId
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
