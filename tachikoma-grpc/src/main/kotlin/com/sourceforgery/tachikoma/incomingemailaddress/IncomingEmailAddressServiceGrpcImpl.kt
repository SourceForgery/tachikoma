package com.sourceforgery.tachikoma.incomingemailaddress

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddressServiceGrpc
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class IncomingEmailAddressServiceGrpcImpl
@Inject
private constructor(
        private val incomingEmailAddressService: IncomingEmailAddressService,
        private val grpcExceptionMap: GrpcExceptionMap
) : IncomingEmailAddressServiceGrpc.IncomingEmailAddressServiceImplBase() {

    override fun addIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) {
        try {
            incomingEmailAddressService.addIncomingEmailAddress(request)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }

    override fun getIncomingEmailAddresses(request: Empty, responseObserver: StreamObserver<IncomingEmailAddress>) {
        try {
            incomingEmailAddressService.getIncomingEmailAddresses(responseObserver)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }

    override fun deleteIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) {
        try {
            incomingEmailAddressService.deleteIncomingEmailAddress(request)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvert(e))
        }
    }
}
