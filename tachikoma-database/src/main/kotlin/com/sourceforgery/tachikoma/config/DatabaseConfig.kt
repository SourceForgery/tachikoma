package com.sourceforgery.tachikoma.config

import java.net.URI

interface DatabaseConfig {
    val createDatabase: Boolean
    val databaseEncryptionKey: String
    val sqlUrl: URI
    val timeDatabaseQueries: Boolean
    val wipeAndCreateDatabase: Boolean
}