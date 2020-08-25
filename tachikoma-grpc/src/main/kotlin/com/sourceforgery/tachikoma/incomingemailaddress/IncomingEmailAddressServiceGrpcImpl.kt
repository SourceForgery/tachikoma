package com.sourceforgery.tachikoma.incomingemailaddress

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddressServiceGrpc
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.provider

internal class IncomingEmailAddressServiceGrpcImpl(
    override val di: DI
) : IncomingEmailAddressServiceGrpc.IncomingEmailAddressServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {

    private val incomingEmailAddressService: IncomingEmailAddressService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authentication: () -> Authentication by provider()

    override fun addIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontendAdmin(auth.mailDomain)
            incomingEmailAddressService.addIncomingEmailAddress(request, auth.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getIncomingEmailAddresses(request: Empty, responseObserver: StreamObserver<IncomingEmailAddress>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontendAdmin(auth.mailDomain)
            incomingEmailAddressService.getIncomingEmailAddresses(responseObserver, auth.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun deleteIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) = grpcFuture(responseObserver) {
        try {
            val auth = authentication()
            auth.requireFrontendAdmin(auth.mailDomain)
            incomingEmailAddressService.deleteIncomingEmailAddress(request, auth.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
