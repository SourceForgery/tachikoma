package com.sourceforgery.tachikoma.webserver.rest

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction
import com.sourceforgery.rest.catchers.RestExceptionMap
import javax.inject.Inject

class RestExceptionHandlerFunction
@Inject
private constructor(
        private val restExceptionMap: RestExceptionMap
) : ExceptionHandlerFunction {
    override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: Throwable): HttpResponse {
        return restExceptionMap[cause::class.java]!!.handleException(ctx, req, cause)
    }
}