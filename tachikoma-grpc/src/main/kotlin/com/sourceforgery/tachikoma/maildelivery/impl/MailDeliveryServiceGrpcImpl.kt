package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.GetIncomingEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.grpcFuture
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
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
    private val incomingEmailService: IncomingEmailService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getIncomingEmails(request: Empty, responseObserver: StreamObserver<IncomingEmail>) =
        streamIncomingEmails(IncomingEmailParameters.getDefaultInstance(), responseObserver)

    override fun streamIncomingEmails(request: IncomingEmailParameters, responseObserver: StreamObserver<IncomingEmail>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            LOGGER.info { "Connected, user ${auth.authenticationId} getting incoming mails from ${auth.mailDomain}" }
            incomingEmailService.streamIncomingEmails(
                responseObserver = responseObserver,
                authenticationId = auth.authenticationId,
                mailDomain = auth.mailDomain,
                accountId = auth.accountId,
                parameters = request
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getIncomingEmail(request: GetIncomingEmailRequest, responseObserver: StreamObserver<IncomingEmail>) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            incomingEmailService.getIncomingEmail(
                incomingEmailId = IncomingEmailId(request.incomingEmailId.id),
                accountId = auth.accountId,
                parameters = request.parameters
            )
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun sendEmail(request: OutgoingEmail, responseObserver: StreamObserver<EmailQueueStatus>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            LOGGER.trace {
                val recipients = request.recipientsList.joinToString { it.namedEmail.email }
                "Starting sendEmail from ${request.from.email} to $recipients for AccountId(${auth.accountId})"
            }
            mailDeliveryService.sendEmail(
                request = request,
                responseObserver = responseObserver,
                authenticationId = auth.authenticationId
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            val convertedException = grpcExceptionMap.findAndConvertAndLog(e)
            if (LOGGER.delegate.isTraceEnabled) {
                LOGGER.trace(convertedException) { "Failed to send email" }
            } else {
                LOGGER.debug { "Failed to send email" }
            }
            responseObserver.onError(convertedException)
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
