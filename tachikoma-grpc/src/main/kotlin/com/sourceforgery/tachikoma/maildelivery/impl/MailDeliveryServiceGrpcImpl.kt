package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

internal class MailDeliveryServiceGrpcImpl
@Inject
private constructor(
    private val mailDeliveryService: MailDeliveryService,
    private val authentication: Authentication,
    private val grpcExceptionMap: GrpcExceptionMap,
    tachikomaScope: TachikomaScope
) : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase(), CoroutineScope by tachikomaScope {
    override fun getIncomingEmails(request: Empty, responseObserver: StreamObserver<IncomingEmail>) = grpcLaunch {
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

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) = grpcLaunch {
        try {
            authentication.requireFrontend()
            mailDeliveryService.sendEmail(
                request = request,
                responseObserver = responseObserver,
                authenticationId = authentication.authenticationId
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
