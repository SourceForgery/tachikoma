package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.net.URI

internal class Configuration : DatabaseConfig, TrackingConfig {
    override val sqlUrl = readConfig("SQL_URL", "postgres://username:password@localhost:5432/tachikoma", URI::class.java)
    override val createDatabase = readConfig("CREATE_DATABASE", true)
    override val trackingEncryptionKey: String = readConfig("TRACKING_ENCRYPTION_KEY", "peilieK6RoomaiPhainocool6ebezai0ox7qui4p")
}