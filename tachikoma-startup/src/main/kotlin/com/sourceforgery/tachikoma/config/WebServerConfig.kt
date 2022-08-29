package com.sourceforgery.tachikoma.config

interface WebServerConfig {
    // The key used to cryptographically sign the authorization tokens
    val webtokenSignKey: ByteArray
    val sslCertChainFile: String
    val sslCertKeyFile: String
    // Overrides X-Forwarded-For when using e.g. Cloudflare or another service
    val overridingClientIpHeader: String
}
