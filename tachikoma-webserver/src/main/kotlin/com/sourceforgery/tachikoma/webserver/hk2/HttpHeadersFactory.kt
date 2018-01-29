package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpRequest
import org.glassfish.hk2.api.Factory
import javax.inject.Inject

class HttpHeadersFactory
@Inject
private constructor(
        private val httpRequest: HttpRequest
) : Factory<HttpHeaders> {
    override fun provide() =
            httpRequest.headers()

    override fun dispose(instance: HttpHeaders?) {
    }
}