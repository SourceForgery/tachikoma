package com.sourceforgery.tachikoma.blockedemail

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmailServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmails
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class BlockedEmailServiceGrpcImpl
@Inject
private constructor(
        private val blockedEmailService: BlockedEmailService,
        private val grpcExceptionMap: GrpcExceptionMap
) : BlockedEmailServiceGrpc.BlockedEmailServiceImplBase() {

    override fun getBlockedEmails(request: Empty, responseObserver: StreamObserver<BlockedEmails>) {
        try {
            blockedEmailService.getBlockedEmails(responseObserver)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}
