package com.sourceforgery.tachikoma.webserver.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus.UNAUTHORIZED
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.exceptions.NoAuthorizationCredentialsException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionCatcher
import io.grpc.Status
import org.kodein.di.DI

class NoAuthorizationCredentialsCatcher(
    override val di: DI,
) : GrpcExceptionCatcher<NoAuthorizationCredentialsException>(NoAuthorizationCredentialsException::class.java),
    RestExceptionCatcher<NoAuthorizationCredentialsException> {
    override fun handleException(
        ctx: RequestContext?,
        req: HttpRequest?,
        cause: NoAuthorizationCredentialsException,
    ) = HttpResponse.of(UNAUTHORIZED)

    override fun status(t: NoAuthorizationCredentialsException) = Status.UNAUTHENTICATED
}
