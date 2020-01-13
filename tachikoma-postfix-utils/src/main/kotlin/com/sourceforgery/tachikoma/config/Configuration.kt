package com.sourceforgery.tachikoma.config

import java.net.URI

internal class Configuration {
    val tachikomaUrl by readConfig(URI(""))
    val insecure by readConfig(false)
}
