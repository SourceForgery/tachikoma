package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.tracking.RemoteIP
import io.netty.util.AsciiString
import java.net.InetSocketAddress
import javax.inject.Inject

class RemoteIPImpl
@Inject
private constructor(
    private val httpRequest: HttpRequest,
    private val requestContext: RequestContext
) : RemoteIP {
    override val remoteAddress: String
        get() {
            return httpRequest.headers().get(X_FORWARDED_FOR)
                ?.substringBefore(',')
                ?: let {
                    requestContext.remoteAddress<InetSocketAddress>()!!
                        .address
                        .hostAddress
                }
        }

    companion object {
        private val X_FORWARDED_FOR = AsciiString.of("X-Forwarded-For")
    }
}