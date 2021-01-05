package com.sourceforgery.tachikoma.webserver.grpc

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.server.DecoratingHttpServiceFunction
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.kodein.DatabaseSessionContext
import com.sourceforgery.tachikoma.kodein.DatabaseSessionKodeinScope
import com.sourceforgery.tachikoma.kodein.threadLocalDatabaseSessionScope
import org.kodein.di.DI
import org.kodein.di.DIAware

class HttpRequestScopedDecorator(override val di: DI) : DecoratingHttpServiceFunction, DIAware {

    override fun serve(delegate: HttpService, ctx: ServiceRequestContext, req: HttpRequest): HttpResponse {
        val databaseSessionContext = DatabaseSessionContext()
        ctx.log()
            .whenComplete()
            .whenComplete { _, _ ->
                threadLocalDatabaseSessionScope.remove()
                DatabaseSessionKodeinScope.getRegistry(databaseSessionContext).close()
            }
        threadLocalDatabaseSessionScope.set(databaseSessionContext)
        return delegate.serve(ctx, req)
    }
}
