package com.sourceforgery.tachikoma.config

import java.net.URI

internal class Configuration {
    val tachikomaUrl = readConfig("TACHIKOMA_URL", "", URI::class.java)
    val insecure = readConfig("INSECURE", false)
}
