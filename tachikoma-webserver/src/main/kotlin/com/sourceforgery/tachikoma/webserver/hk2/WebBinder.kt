package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.hk2.ReferencingFactory
import com.sourceforgery.tachikoma.hk2.RequestScoped
import com.sourceforgery.tachikoma.webserver.AuthenticationFactory
import org.glassfish.hk2.utilities.binding.AbstractBinder

class WebBinder : AbstractBinder() {
    override fun configure() {
        bindFactory(ReferencingFactory<HttpRequest>())
                .to(HTTP_REQUEST_TYPE)
                .`in`(RequestScoped::class.java)
        bindFactory(ReferencingFactory<RequestContext>())
                .to(REQUEST_CONTEXT_TYPE)
                .`in`(RequestScoped::class.java)
        bindFactory(HttpRequestFactory::class.java)
                .to(HttpRequest::class.java)
                .`in`(RequestScoped::class.java)
        bindFactory(ServiceRequestContextFactory::class.java)
                .to(RequestContext::class.java)
                .`in`(RequestScoped::class.java)
        bindFactory(HttpHeadersFactory::class.java)
                .to(HttpHeaders::class.java)
                .`in`(RequestScoped::class.java)
        bindFactory(AuthenticationFactory::class.java)
                .to(Authentication::class.java)
                .`in`(RequestScoped::class.java)
    }
}