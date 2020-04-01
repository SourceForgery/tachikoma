package com.sourceforgery.tachikoma.config

import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeConfig
import java.net.URI

internal class Configuration :
    DatabaseConfig,
    DebugConfig,
    MqConfig,
    TrackingConfig,
    UnsubscribeConfig,
    WebServerConfig,
    WebtokenAuthConfig {
    override val databaseEncryptionKey by readEncryptionConfig("DATABASE_ENCRYPTION_KEY")
    override val mqUrl by readConfig("MQ_URL", URI("amqp://guest:guest@localhost/tachikoma"))
    override val sendDebugData: Boolean by readConfig("SEND_DEBUG_DATA", true)
    override val sqlUrl by readConfig("SQL_URL", URI("postgres://username:password@localhost:5432/tachikoma"))
    override val timeDatabaseQueries: Boolean by readConfig("TIME_DATABASE_QUERIES", true)
    override val linkSignKey by readEncryptionConfig("LINK_SIGN_KEY", byteArrayOf())
    override val webtokenSignKey by readEncryptionConfig("WEBTOKEN_SIGN_KEY", byteArrayOf())
    override val baseUrl: URI by readConfig("BASE_URL", URI("http://localhost:8070/"))
    override val sslCertChainFile by readConfig("SSL_CERT_CHAIN_FILE", "")
    override val sslCertKeyFile by readConfig("SSL_CERT_KEY_FILE", "")
    override val mailDomains by readListConfig("MAIL_DOMAINS", listOf(MailDomain("example.com")))
    override val unsubscribeDomainOverride by readConfig("UNSUBSCRIBE_DOMAIN_OVERRIDE", null as MailDomain?)
}
