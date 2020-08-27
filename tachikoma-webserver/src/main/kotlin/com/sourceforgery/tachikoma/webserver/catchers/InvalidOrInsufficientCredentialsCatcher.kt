package com.sourceforgery.tachikoma.webserver.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus.FORBIDDEN
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionCatcher
import io.grpc.Status
import org.kodein.di.DI

class InvalidOrInsufficientCredentialsCatcher(override val di: DI) : GrpcExceptionCatcher<InvalidOrInsufficientCredentialsException>(InvalidOrInsufficientCredentialsException::class.java),
    RestExceptionCatcher<InvalidOrInsufficientCredentialsException> {

    override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: InvalidOrInsufficientCredentialsException) =
        HttpResponse.of(FORBIDDEN)

    override fun status(t: InvalidOrInsufficientCredentialsException) =
        Status.PERMISSION_DENIED.augmentDescription(t.message)

    override fun logError(t: InvalidOrInsufficientCredentialsException) {
        logger.warn { t.message }
    }
}
