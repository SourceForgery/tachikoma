package com.sourceforgery.tachikoma.config

import java.net.URI

internal class Configuration {
    val tachikomaUrl by readConfig("TACHIKOMA_URL", URI(""))
    val insecure by readConfig("INSECURE", false)
}
