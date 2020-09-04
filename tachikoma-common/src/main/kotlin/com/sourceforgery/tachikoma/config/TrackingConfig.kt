package com.sourceforgery.tachikoma.config

import java.net.URI

interface TrackingConfig {
    val linkSignKey: ByteArray
    val baseUrl: URI
}
