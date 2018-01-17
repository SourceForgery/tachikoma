package com.sourceforgery.tachikoma.webserver.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus.UNAUTHORIZED
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.rest.catchers.RestExceptionCatcher
import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.exceptions.NoAuthorizationCredentialsException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import io.grpc.Status
import javax.inject.Inject

class NoAuthorizationCredentialsCatcher
@Inject
private constructor(
        debugConfig: DebugConfig
) : GrpcExceptionCatcher<NoAuthorizationCredentialsException>(debugConfig, NoAuthorizationCredentialsException::class.java), RestExceptionCatcher<NoAuthorizationCredentialsException> {
    override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: NoAuthorizationCredentialsException) =
            HttpResponse.of(UNAUTHORIZED)

    override fun status(t: NoAuthorizationCredentialsException) =
            Status.UNAUTHENTICATED
}