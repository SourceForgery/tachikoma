package com.sourceforgery.tachikoma.config

interface WebServerConfig {
    val webtokenSignKey: ByteArray
    val sslCertChainFile: String
    val sslCertKeyFile: String
}
