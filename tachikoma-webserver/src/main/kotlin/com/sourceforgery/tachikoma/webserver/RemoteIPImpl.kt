package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.tracking.RemoteIP
import io.netty.util.AsciiString
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.provider
import java.net.InetSocketAddress

class RemoteIPImpl(override val di: DI) : RemoteIP, DIAware {
    private val httpHeaders: () -> HttpHeaders by provider()
    private val requestContext: () -> RequestContext by provider()

    override val remoteAddress: String
        get() {
            return httpHeaders().get(X_FORWARDED_FOR)
                ?.substringBefore(',')
                ?: let {
                    requestContext().remoteAddress<InetSocketAddress>()!!
                        .address
                        .hostAddress
                }
        }

    companion object {
        private val X_FORWARDED_FOR = AsciiString.of("X-Forwarded-For")
    }
}
