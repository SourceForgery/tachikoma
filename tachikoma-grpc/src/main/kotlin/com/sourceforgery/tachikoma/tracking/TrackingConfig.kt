package com.sourceforgery.tachikoma.tracking

import java.net.URI

interface TrackingConfig {
    val linkSignKey: String
    val baseUrl: URI
}
