package com.sourceforgery.tachikoma.blockedemail

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmailServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class BlockedEmailServiceGrpcImpl(
    override val di: DI
) : BlockedEmailServiceGrpc.BlockedEmailServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val authentication: () -> Authentication by provider()
    private val blockedEmailService: BlockedEmailService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getBlockedEmails(request: Empty, responseObserver: StreamObserver<BlockedEmail>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            blockedEmailService.getBlockedEmails(responseObserver, auth.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun removeBlockedEmail(request: RemoveBlockedEmailRequest, responseObserver: StreamObserver<Empty>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontend()
            blockedEmailService.removeBlockedEmail(request, auth.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
