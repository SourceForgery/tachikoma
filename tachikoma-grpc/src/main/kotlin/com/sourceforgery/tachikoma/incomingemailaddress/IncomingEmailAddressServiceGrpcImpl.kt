package com.sourceforgery.tachikoma.incomingemailaddress

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddressServiceGrpc
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class IncomingEmailAddressServiceGrpcImpl
@Inject
private constructor(
        private val incomingEmailAddressService: IncomingEmailAddressService,
        private val grpcExceptionMap: GrpcExceptionMap,
        private val authentication: Authentication
) : IncomingEmailAddressServiceGrpc.IncomingEmailAddressServiceImplBase() {

    override fun addIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) {
        try {
            authentication.requireFrontendAdmin()
            incomingEmailAddressService.addIncomingEmailAddress(request, authentication.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getIncomingEmailAddresses(request: Empty, responseObserver: StreamObserver<IncomingEmailAddress>) {
        try {
            authentication.requireFrontendAdmin()
            incomingEmailAddressService.getIncomingEmailAddresses(responseObserver, authentication.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun deleteIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) {
        try {
            authentication.requireFrontendAdmin()
            incomingEmailAddressService.deleteIncomingEmailAddress(request, authentication.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
