package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.net.URI
import java.nio.charset.StandardCharsets

internal class Configuration : DatabaseConfig, TrackingConfig, MqConfig, WebServerConfig, DebugConfig, WebtokenAuthConfig {
    override val databaseEncryptionKey: String = readEncryptionConfig("DATABASE_ENCRYPTION_KEY")
    override val mqUrl: URI = readConfig("MQ_URL", "amqp://guest:guest@localhost/tachikoma", URI::class.java)
    override val sendDebugData: Boolean = readConfig("SEND_DEBUG_DATA", true)
    override val sqlUrl = readConfig("SQL_URL", "postgres://username:password@localhost:5432/tachikoma", URI::class.java)
    override val timeDatabaseQueries: Boolean = readConfig("TIME_DATABASE_QUERIES", true)
    override val linkSignKey: String = readEncryptionConfig("LINK_SIGN_KEY")
    override val webtokenSignKey: ByteArray = readEncryptionConfig("WEBTOKEN_SIGN_KEY").toByteArray(StandardCharsets.UTF_8)
    override val baseUrl: URI = readConfig("BASE_URL", "http://localhost:8070/", URI::class.java)
    override val createDatabase: Boolean = false
    override val sslCertChainFile = readConfig("SSL_CERT_CHAIN_FILE", "")
    override val sslCertKeyFile = readConfig("SSL_CERT_KEY_FILE", "")
}
