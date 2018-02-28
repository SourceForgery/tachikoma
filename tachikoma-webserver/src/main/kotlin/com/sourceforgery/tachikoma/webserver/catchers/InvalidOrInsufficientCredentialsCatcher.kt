package com.sourceforgery.tachikoma.webserver.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus.FORBIDDEN
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionCatcher
import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import io.grpc.Status
import javax.inject.Inject

class InvalidOrInsufficientCredentialsCatcher
@Inject
private constructor(
        debugConfig: DebugConfig
) : GrpcExceptionCatcher<InvalidOrInsufficientCredentialsException>(debugConfig, InvalidOrInsufficientCredentialsException::class.java), RestExceptionCatcher<InvalidOrInsufficientCredentialsException> {
    override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: InvalidOrInsufficientCredentialsException) =
            HttpResponse.of(FORBIDDEN)

    override fun status(t: InvalidOrInsufficientCredentialsException) =
            Status.PERMISSION_DENIED
}