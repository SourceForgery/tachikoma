package com.sourceforgery.tachikoma.config

import java.net.URI

interface DatabaseConfig {
    val databaseEncryptionKey: String
    val sqlUrl: URI
    val timeDatabaseQueries: Boolean
    val createDatabase: Boolean
}