package com.sourceforgery.tachikoma.tracking

import java.net.URI

interface TrackingConfig {
    val trackingEncryptionKey: String
    // TODO Move to other config?
    val baseUrl: URI
}
