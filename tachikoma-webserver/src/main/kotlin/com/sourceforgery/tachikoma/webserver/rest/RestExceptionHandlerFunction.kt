package com.sourceforgery.tachikoma.webserver.rest

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.annotation.ExceptionHandlerFunction
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionMap
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class RestExceptionHandlerFunction(
    override val di: DI,
) : ExceptionHandlerFunction, DIAware {
    private val restExceptionMap: RestExceptionMap by instance()

    override fun handleException(
        ctx: ServiceRequestContext,
        req: HttpRequest,
        cause: Throwable,
    ): HttpResponse {
        @Suppress("UNCHECKED_CAST")
        return restExceptionMap.findCatcher(cause::class.java as Class<Throwable>).handleException(ctx, req, cause)
    }
}
