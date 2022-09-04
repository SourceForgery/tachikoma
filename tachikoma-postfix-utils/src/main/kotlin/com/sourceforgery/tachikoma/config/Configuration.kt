package com.sourceforgery.tachikoma.config

import java.net.URI

interface GrpcClientConfig {
    val tachikomaUrl: URI
    val insecure: Boolean
    val clientCert: String
    val clientKey: String
}

internal class Configuration : GrpcClientConfig {
    override val tachikomaUrl by readConfig("TACHIKOMA_URL", URI(""))
    override val insecure by readConfig("INSECURE", false)
    override val clientCert by readConfig("TACHIKOMA_CLIENT_CERT", "")
    override val clientKey by readConfig("TACHIKOMA_CLIENT_KEY", "")
}
