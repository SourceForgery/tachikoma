package com.sourceforgery.tachikoma.webserver.grpc

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.HK2RequestContextImpl
import com.sourceforgery.tachikoma.hk2.SettableReference
import com.sourceforgery.tachikoma.webserver.hk2.HTTP_REQUEST_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.REQUEST_CONTEXT_TYPE
import javax.inject.Inject
import org.glassfish.hk2.api.ServiceLocator

class HttpRequestScopedDecorator
@Inject
private constructor(
    private val hK2RequestContext: HK2RequestContextImpl,
    private val serviceLocator: ServiceLocator
) : DecoratingHttpServiceFunction {
    override fun serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
        val hk2Ctx = hK2RequestContext.createInArmeriaContext(ctx) as HK2RequestContextImpl.Instance
        ctx.log()
            .whenComplete()
            .whenComplete { log, _ -> hK2RequestContext.release(hk2Ctx) }
        serviceLocator
                .getService<SettableReference<HttpRequest>>(HTTP_REQUEST_TYPE)
                .value = req
        serviceLocator
                .getService<SettableReference<RequestContext>>(REQUEST_CONTEXT_TYPE)
                .value = ctx
        return delegate.serve(ctx, req)
    }
}