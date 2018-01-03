package com.sourceforgery.tachikoma.webserver.hk2;

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.ReferencingFactory
import org.glassfish.hk2.utilities.binding.AbstractBinder

class WebBinder : AbstractBinder() {
    override fun configure() {
        bindFactory(ReferencingFactory<HttpRequest>())
                .to(HTTP_REQUEST_TYPE)
        bindFactory(ReferencingFactory<ServiceRequestContext>())
                .to(REQUEST_CONTEXT_TYPE)
    }
}