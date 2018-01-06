package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.net.URI
import java.nio.charset.StandardCharsets

internal class Configuration : DatabaseConfig, TrackingConfig, MqConfig, WebServerConfig {
    override val createDatabase = readConfig("CREATE_DATABASE", true)
    override val mqUrl: URI = readConfig("MQ_URL", "amqp://guest:guest@localhost/tachikoma", URI::class.java)
    override val sqlUrl = readConfig("SQL_URL", "postgres://username:password@localhost:5432/tachikoma", URI::class.java)
    override val timeDatabaseQueries: Boolean = readConfig("TIME_DATABASE_QUERIES", true)
    override val trackingEncryptionKey: String = readConfig("TRACKING_ENCRYPTION_KEY", "peilieK6RoomaiPhainocool6ebezai0ox7qui4p")
    override val webtokenSignKey: ByteArray = readConfig("WEBTOKEN_SIGN_KEY", "really, really poor key").toByteArray(StandardCharsets.UTF_8)
    override val wipeAndCreateDatabase: Boolean = readConfig("WIPE_AND_CREATE_DATABASE", false)
}