package com.sourceforgery.tachikoma.config

import java.net.URI

class Configuration : DatabaseConfig {
    override val sqlUrl = readConfig("SQL_URL", "postgres://username:password@localhost:5432/tachikoma", URI::class.java)
    override val createDatabase = readConfig("CREATE_DATABASE", true)
}