package com.sourceforgery.tachikoma.incomingemailaddress

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmailServiceGrpc
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddressServiceGrpc
import com.sourceforgery.tachikoma.grpc.grpcLaunch
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class IncomingEmailAddressServiceGrpcImpl
@Inject
private constructor(
    private val incomingEmailAddressService: IncomingEmailAddressService,
    private val grpcExceptionMap: GrpcExceptionMap,
    private val authentication: Authentication,
    tachikomaScope: TachikomaScope
) : IncomingEmailAddressServiceGrpc.IncomingEmailAddressServiceImplBase(), CoroutineScope by tachikomaScope {

    override fun addIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) = grpcLaunch {
        try {
            authentication.requireFrontendAdmin(authentication.mailDomain)
            incomingEmailAddressService.addIncomingEmailAddress(request, authentication.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun getIncomingEmailAddresses(request: Empty, responseObserver: StreamObserver<IncomingEmailAddress>) = grpcLaunch {
        try {
            authentication.requireFrontendAdmin(authentication.mailDomain)
            incomingEmailAddressService.getIncomingEmailAddresses(responseObserver, authentication.authenticationId)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }

    override fun deleteIncomingEmailAddress(request: IncomingEmailAddress, responseObserver: StreamObserver<Empty>) = grpcLaunch {
        try {
            authentication.requireFrontendAdmin(authentication.mailDomain)
            incomingEmailAddressService.deleteIncomingEmailAddress(request, authentication.authenticationId)
            responseObserver.onNext(Empty.getDefaultInstance())
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
