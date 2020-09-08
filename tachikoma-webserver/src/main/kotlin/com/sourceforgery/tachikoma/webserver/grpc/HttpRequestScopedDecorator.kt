package com.sourceforgery.tachikoma.webserver.grpc

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.database.server.LogEverythingFactory
import com.sourceforgery.tachikoma.kodein.threadLocalLogEverything
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class HttpRequestScopedDecorator(override val di: DI) : DecoratingHttpServiceFunction, DIAware {
    private val logEverythingFactory: LogEverythingFactory by instance()

    override fun serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
        val logEverything = logEverythingFactory.create()
        ctx.log()
            .whenComplete()
            .whenComplete { _, _ -> threadLocalLogEverything.remove() }
        threadLocalLogEverything.set(logEverything)
        return delegate.serve(ctx, req)
    }
}
