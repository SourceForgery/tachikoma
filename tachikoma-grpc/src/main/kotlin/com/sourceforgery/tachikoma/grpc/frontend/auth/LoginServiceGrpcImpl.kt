package com.sourceforgery.tachikoma.grpc.frontend.auth

import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.grpcFuture
import io.grpc.stub.StreamObserver
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance

class LoginServiceGrpcImpl(
    override val di: DI
) : LoginServiceGrpc.LoginServiceImplBase(),
    DIAware,
    TachikomaScope by di.direct.instance() {
    private val loginService: LoginService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    override fun login(request: LoginRequest, responseObserver: StreamObserver<LoginResponse>) = grpcFuture(responseObserver) {
        try {
            val response = loginService.login(request)
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            responseObserver.onError(grpcExceptionMap.findAndConvertAndLog(e))
        }
    }
}
