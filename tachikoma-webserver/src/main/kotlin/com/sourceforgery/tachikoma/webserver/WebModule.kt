package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.kodein.threadLocalRequestContext
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.webserver.catchers.InvalidOrInsufficientCredentialsCatcher
import com.sourceforgery.tachikoma.webserver.catchers.NoAuthorizationCredentialsCatcher
import com.sourceforgery.tachikoma.webserver.catchers.NotFoundCatcher
import com.sourceforgery.tachikoma.webserver.graphql.GraphqlServiceProvider
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton

val webModule = DI.Module("web") {
    bind<RequestContext>() with provider {
        threadLocalRequestContext.get()
            ?: RequestContext.current()
            ?: error("Not in http request context")
    }
    bind<HttpRequest>() with provider { instance<RequestContext>().request() ?: error("Not http request?") }
    bind<HttpHeaders>() with provider { instance<HttpRequest>().headers() }
    bind<AuthenticationFactory>() with singleton { AuthenticationFactory(di) }
    bind<Authentication>() with provider { instance<AuthenticationFactory>().provide() }

    importOnce(catchersModule)

    bind<GrpcExceptionInterceptor>() with singleton { GrpcExceptionInterceptor(di) }
    bind<RestExceptionHandlerFunction>() with singleton { RestExceptionHandlerFunction(di) }
    bind<RemoteIP>() with singleton { RemoteIPImpl(di) }
    bind<GraphqlServiceProvider>() with singleton { GraphqlServiceProvider(di) }
}

private val catchersModule = DI.Module("catchers") {
    bind<InvalidOrInsufficientCredentialsCatcher>() with singleton { InvalidOrInsufficientCredentialsCatcher(di) }
    bind<NoAuthorizationCredentialsCatcher>() with singleton { NoAuthorizationCredentialsCatcher(di) }
    bind<NotFoundCatcher>() with singleton { NotFoundCatcher(di) }
}
