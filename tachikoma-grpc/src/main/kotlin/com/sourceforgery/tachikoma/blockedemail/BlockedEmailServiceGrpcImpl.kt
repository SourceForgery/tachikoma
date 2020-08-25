package com.sourceforgery.tachikoma.blockedemail

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmailServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

internal class BlockedEmailServiceGrpcImpl
@Inject
private constructor(
    private val authentication: Authentication,
    private val blockedEmailService: BlockedEmailService,
    private val grpcExceptionMap: GrpcExceptionMap,
    tachikomaScope: TachikomaScope
) : BlockedEmailServiceGrpc.BlockedEmailServiceImplBase(), CoroutineScope by tachikomaScope {

    override fun getBlockedEmails(request: Empty, responseObserver: StreamObserver<BlockedEmail>) = grpcLaunch {
        try {
            authentication.requireFrontend()
            blockedEmailService.getBlockedEmails(responseObserver, authentication.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun removeBlockedEmail(request: RemoveBlockedEmailRequest, responseObserver: StreamObserver<Empty>) = grpcLaunch {
        try {
            authentication.requireFrontend()
            blockedEmailService.removeBlockedEmail(request, authentication.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
