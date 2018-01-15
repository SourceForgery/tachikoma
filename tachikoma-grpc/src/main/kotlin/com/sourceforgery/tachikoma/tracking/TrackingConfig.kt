package com.sourceforgery.tachikoma.tracking

import java.net.URI

interface TrackingConfig {
    val encryptionKey: String
    val baseUrl: URI
}
