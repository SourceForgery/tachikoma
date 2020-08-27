package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class MailDeliveryServiceGrpcImpl(override val di: DI) : MailDeliveryServiceGrpc.MailDeliveryServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val mailDeliveryService: MailDeliveryService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getIncomingEmails(request: Empty, responseObserver: StreamObserver<IncomingEmail>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            LOGGER.info { "Connected, user ${auth.authenticationId} getting incoming mails from ${auth.mailDomain}" }
            mailDeliveryService.getIncomingEmails(
                responseObserver = responseObserver,
                authenticationId = auth.authenticationId,
                mailDomain = auth.mailDomain,
                accountId = auth.accountId
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            mailDeliveryService.sendEmail(
                request = request,
                responseObserver = responseObserver,
                authenticationId = auth.authenticationId
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
