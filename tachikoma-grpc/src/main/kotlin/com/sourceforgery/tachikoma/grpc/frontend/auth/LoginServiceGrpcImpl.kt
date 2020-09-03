package com.sourceforgery.tachikoma.grpc.frontend.auth

import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class LoginServiceGrpcImpl(
    override val di: DI
) : LoginServiceGrpcKt.LoginServiceCoroutineImplBase(), DIAware {
    private val loginService: LoginService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override suspend fun login(request: LoginRequest): LoginResponse {
        try {
            return withContext(Dispatchers.Default) {
                loginService.login(request)
            }
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }
}
