package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpHeaderNames.X_FORWARDED_FOR
import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.tracking.RemoteIP
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

class RemoteIPImpl(override val di: DI) : RemoteIP, DIAware {
    private val httpHeaders: () -> HttpHeaders by provider()
    private val requestContext: () -> RequestContext by provider()
    private val webServerConfig: WebServerConfig by instance()

    override val remoteAddress: String
        get() {
            val headers = httpHeaders()
            return overridingHeader(headers)
                ?: forwardedFor(headers)
                ?: remoteSocket(requestContext())
        }

    fun overridingHeader(httpHeaders: HttpHeaders) =
        if (webServerConfig.overridingClientIpHeader.isNotBlank()) {
            httpHeaders.get(webServerConfig.overridingClientIpHeader)
        } else {
            null
        }

    fun forwardedFor(httpHeaders: HttpHeaders) =
        httpHeaders.get(X_FORWARDED_FOR)
            ?.substringBefore(',')
            .takeUnless { it.isNullOrBlank() }

    fun remoteSocket(requestContext: RequestContext) =
        requestContext.remoteAddress()!!
            .address
            .hostAddress
}
