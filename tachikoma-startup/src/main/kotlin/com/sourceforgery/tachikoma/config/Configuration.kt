package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import java.net.URI

internal class Configuration : DatabaseConfig, TrackingConfig, MqConfig, WebServerConfig, DebugConfig, WebtokenAuthConfig {
    override val databaseEncryptionKey by readEncryptionConfig()
    override val mqUrl by readConfig(URI("amqp://guest:guest@localhost/tachikoma"))
    override val sendDebugData: Boolean by readConfig(true)
    override val sqlUrl by readConfig(URI("postgres://username:password@localhost:5432/tachikoma"))
    override val timeDatabaseQueries: Boolean by readConfig(true)
    override val linkSignKey by readEncryptionConfig(byteArrayOf())
    override val webtokenSignKey by readEncryptionConfig(byteArrayOf())
    override val baseUrl: URI by readConfig(URI("http://localhost:8070/"))
    override val sslCertChainFile by readConfig("")
    override val sslCertKeyFile by readConfig("")
    override val mailDomain: MailDomain by readConfig(MailDomain("example.com"))
}
