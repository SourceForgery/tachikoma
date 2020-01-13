package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.net.URI

interface DatabaseConfig {
    val databaseEncryptionKey: String
    val sqlUrl: URI
    val timeDatabaseQueries: Boolean
    val mailDomain: MailDomain
}