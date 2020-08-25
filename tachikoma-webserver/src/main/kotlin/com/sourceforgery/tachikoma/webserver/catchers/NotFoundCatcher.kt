package com.sourceforgery.tachikoma.webserver.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus.NOT_FOUND
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.exceptions.NotFoundException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionCatcher
import io.grpc.Status
import org.kodein.di.DI

class NotFoundCatcher(
    override val di: DI
) : GrpcExceptionCatcher<NotFoundException>(NotFoundException::class.java),
    RestExceptionCatcher<NotFoundException> {

    override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: NotFoundException) =
        HttpResponse.of(NOT_FOUND)

    override fun status(t: NotFoundException) =
        Status.NOT_FOUND
}
