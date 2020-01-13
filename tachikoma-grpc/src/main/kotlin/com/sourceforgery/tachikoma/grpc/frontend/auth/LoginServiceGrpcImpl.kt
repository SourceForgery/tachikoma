package com.sourceforgery.tachikoma.grpc.frontend.auth

import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import io.grpc.stub.StreamObserver
import javax.inject.Inject

class LoginServiceGrpcImpl
@Inject
private constructor(
    private val loginService: LoginService,
    private val grpcExceptionMap: GrpcExceptionMap
) : LoginServiceGrpc.LoginServiceImplBase() {
    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) {
        try {
            val response = loginService.login(request)
            responseObserver.onNext(response)
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
