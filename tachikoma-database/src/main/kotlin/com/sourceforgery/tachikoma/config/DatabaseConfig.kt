package com.sourceforgery.tachikoma.config

import java.net.URI

interface DatabaseConfig {
    val sqlUrl: URI
    val createDatabase: Boolean
    val timeDatabaseQueries: Boolean
    val wipeAndCreateDatabase: Boolean
}