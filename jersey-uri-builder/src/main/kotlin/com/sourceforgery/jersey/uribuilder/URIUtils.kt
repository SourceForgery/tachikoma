package com.sourceforgery.jersey.uribuilder

import org.apache.logging.log4j.kotlin.loggerOf
import java.net.URI

fun URI.withoutPassword(): URI = JerseyUriBuilder(this)
    .userInfo("REDACTED")
    .build()

fun URI.addPort(): URI {
    val port: Int =
        if (port == -1) {
            when (scheme) {
                "http" -> 80
                "https" -> 443
                else -> throw IllegalArgumentException("$scheme proto's default port is unknown")
            }
        } else {
            port
        }
    return JerseyUriBuilder(this)
        .port(port)
        .build()
}

private object URIUtils

private val LOGGER = loggerOf(URIUtils::class.java)

fun URI.ensureGproto(): URI =
    if (scheme.startsWith("gproto+http")) {
        this
    } else {
        LOGGER.error { "Scheme for ${this.withoutPassword()} is wrong. Scheme must be gproto+http or gproto+https." }
        JerseyUriBuilder(this).scheme("gproto+$scheme").build()
    }
