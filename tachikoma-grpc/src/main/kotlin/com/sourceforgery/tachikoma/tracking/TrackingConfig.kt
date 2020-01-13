package com.sourceforgery.tachikoma.tracking

import java.net.URI

interface TrackingConfig {
    val linkSignKey: ByteArray
    val baseUrl: URI
}
