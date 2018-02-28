package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.rest.catchers.RestExceptionCatcher
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionCatcher
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.HK2RequestContextImpl
import com.sourceforgery.tachikoma.hk2.ReferencingFactory
import com.sourceforgery.tachikoma.hk2.RequestScoped
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.webserver.AuthenticationFactory
import com.sourceforgery.tachikoma.webserver.RemoteIPImpl
import com.sourceforgery.tachikoma.webserver.catchers.InvalidOrInsufficientCredentialsCatcher
import com.sourceforgery.tachikoma.webserver.catchers.NoAuthorizationCredentialsCatcher
import com.sourceforgery.tachikoma.webserver.catchers.NotFoundCatcher
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.TypeLiteral
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class WebBinder : AbstractBinder() {
    override fun configure() {
        bindFactory(ReferencingFactory<HttpRequest>())
                .to(HTTP_REQUEST_TYPE)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindFactory(ReferencingFactory<RequestContext>())
                .to(REQUEST_CONTEXT_TYPE)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindFactory(HttpRequestFactory::class.java)
                .to(HttpRequest::class.java)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindFactory(ServiceRequestContextFactory::class.java)
                .to(RequestContext::class.java)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindFactory(HttpHeadersFactory::class.java)
                .to(HttpHeaders::class.java)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindFactory(AuthenticationFactory::class.java)
                .to(Authentication::class.java)
                .proxy(true)
                .`in`(RequestScoped::class.java)
        bindAsContract(HK2RequestContextImpl::class.java)
                .to(HK2RequestContext::class.java)
                .to(RequestScoped::class.java)
                .to(object : TypeLiteral<Context<RequestScoped>>() {}.type)
                .`in`(Singleton::class.java)
        bindAsContract(HttpRequestScopedDecorator::class.java)
                .`in`(Singleton::class.java)
        bindCatchers()
        bindAsContract(GrpcExceptionInterceptor::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(RestExceptionHandlerFunction::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(RemoteIPImpl::class.java)
                .to(RemoteIP::class.java)
                .`in`(Singleton::class.java)
    }

    private fun bindCatchers() {
        val classes = listOf(
                InvalidOrInsufficientCredentialsCatcher::class.java,
                NoAuthorizationCredentialsCatcher::class.java,
                NotFoundCatcher::class.java
        )
        for (clazz in classes) {
            bindAsContract(clazz)
                    .to(GrpcExceptionCatcher::class.java)
                    .to(RestExceptionCatcher::class.java)
                    .`in`(Singleton::class.java)
        }
    }
}
