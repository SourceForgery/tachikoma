package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpRequest
import com.sourceforgery.tachikoma.hk2.SettableReference
import javax.inject.Inject
import org.glassfish.hk2.api.Factory

class HttpRequestFactory
@Inject
private constructor(
    private val httpRequest: SettableReference<HttpRequest>
) : Factory<HttpRequest> {

    override fun provide(): HttpRequest = httpRequest.value!!

    override fun dispose(instance: HttpRequest?) {
    }
}