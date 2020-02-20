package com.sourceforgery.jersey.uribuilder

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
