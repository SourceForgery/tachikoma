package com.sourceforgery.tachikoma.webserver.grpc

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext
import com.linecorp.armeria.common.logging.RequestLogAvailability
import com.linecorp.armeria.server.DecoratingServiceFunction
import com.linecorp.armeria.server.Service
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.HK2RequestContextImpl
import com.sourceforgery.tachikoma.hk2.SettableReference
import com.sourceforgery.tachikoma.webserver.hk2.HTTP_REQUEST_TYPE
import com.sourceforgery.tachikoma.webserver.hk2.REQUEST_CONTEXT_TYPE
import io.netty.util.AttributeKey
import java.util.function.Consumer
import javax.inject.Inject
import org.glassfish.hk2.api.ServiceLocator

class HttpRequestScopedDecorator
@Inject
private constructor(
    private val hK2RequestContext: HK2RequestContextImpl,
    private val serviceLocator: ServiceLocator
) : DecoratingServiceFunction<HttpRequest, HttpResponse> {
    override fun serve(delegate: Service<HttpRequest, HttpResponse>, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
        val oldHk2Ctx = hK2RequestContext.retrieveCurrent()
        ctx.attr(OLD_HK2_CONTEXT_KEY).set(oldHk2Ctx)
        val hk2Ctx = hK2RequestContext.createInstance()
        ctx.attr(HK2_CONTEXT_KEY).set(hk2Ctx)
        ctx.onEnter(Consumer {
            hK2RequestContext.setCurrent(hk2Ctx)
        })
        ctx.onExit(Consumer {
            hK2RequestContext.resumeCurrent(oldHk2Ctx)
        })
        ctx.log().addListener({ hK2RequestContext.release(hk2Ctx) }, RequestLogAvailability.COMPLETE)
        return hK2RequestContext.runInScope(hk2Ctx,
            {
                serviceLocator
                    .getService<SettableReference<HttpRequest>>(HTTP_REQUEST_TYPE)
                    .value = req
                serviceLocator
                    .getService<SettableReference<RequestContext>>(REQUEST_CONTEXT_TYPE)
                    .value = ctx
                delegate.serve(ctx, req)
            }
        )
    }

    companion object {
        private val HK2_CONTEXT_KEY = AttributeKey.valueOf<HK2RequestContextImpl.Instance>("HK2_CONTEXT")
        private val OLD_HK2_CONTEXT_KEY = AttributeKey.valueOf<HK2RequestContextImpl.Instance>("OLD_HK2_CONTEXT")
    }
}