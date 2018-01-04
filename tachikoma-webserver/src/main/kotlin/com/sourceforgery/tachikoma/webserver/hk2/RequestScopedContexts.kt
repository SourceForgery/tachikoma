package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.Service
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.SimpleDecoratingService
import com.sourceforgery.tachikoma.hk2.RequestContext
import org.glassfish.hk2.api.TypeLiteral
import java.util.concurrent.atomic.AtomicReference

class RequestScopedService(service: Service<HttpRequest, HttpResponse>, val requestContext: RequestContext) : SimpleDecoratingService<HttpRequest, HttpResponse>(service) {
    override fun serve(ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
        return requestContext.runInScope { locator ->
            locator.getService<AtomicReference<HttpRequest>>(HTTP_REQUEST_TYPE).set(req)
            locator.getService<AtomicReference<ServiceRequestContext>>(REQUEST_CONTEXT_TYPE).set(ctx)
            delegate<Service<HttpRequest, HttpResponse>>().serve(ctx, req)
        }
    }
}

internal val HTTP_REQUEST_TYPE = object : TypeLiteral<AtomicReference<HttpRequest>>() {}.type
internal val REQUEST_CONTEXT_TYPE = object : TypeLiteral<AtomicReference<ServiceRequestContext>>() {}.type