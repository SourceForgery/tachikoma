package com.sourceforgery.tachikoma.postfix

import com.sourceforgery.tachikoma.incoming.IncomingEmail
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.syslog.Syslogger
import io.grpc.Metadata
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.grpc.stub.MetadataUtils
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import java.net.URI
import java.util.concurrent.TimeUnit

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

class Main(
        urlWithoutDomain: URI,
        private val mailDomain: String,
        private val insecure: Boolean
) {

    private val tachikomaUrl = addDomain(urlWithoutDomain)

    private fun withoutPassword(uri: URI): String {
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        return "${uri.scheme}://${uri.host}${uri.path ?: "/"}$query"
    }

    private fun addDomain(uri: URI): URI {
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        val port: Int =
                if (uri.port == -1) {
                    when (uri.scheme) {
                        "http" -> 80
                        "https" -> 443
                        else -> throw IllegalArgumentException("Unknown proto's default port is unknown")
                    }
                } else {
                    uri.port
                }
        return URI.create("${uri.scheme}://$mailDomain:${uri.userInfo}@${uri.host}:$port${uri.path ?: "/"}$query")
    }

    fun run() {
        LOGGER.info { "Connecting to ${withoutPassword(tachikomaUrl)}" }

        val plaintext = tachikomaUrl.scheme == "http"

        val metadataAuth = Metadata()
        metadataAuth.put(APITOKEN_HEADER, tachikomaUrl.userInfo)

        val channel = NettyChannelBuilder.forAddress(tachikomaUrl.host, tachikomaUrl.port)
                .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
                .apply {
                    if (plaintext) {
                        usePlaintext(true)
                    } else {
                        useTransportSecurity()
                        sslContext(
                                GrpcSslContexts.forClient()
                                        .also { ctx ->
                                            if (insecure) {
                                                ctx.trustManager(InsecureTrustManagerFactory.INSTANCE)
                                            }
                                        }
                                        .build()
                        )
                    }
                }
                .idleTimeout(365, TimeUnit.DAYS)
                .build()
        MailSender(channel).start()
        IncomingEmail(channel).start()
        Syslogger(channel).blockingSniffer()
    }

    companion object {
        private val LOGGER = logger()
    }
}

fun main(args: Array<String>) {
    InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE)

    val mailDomain = System.getenv("MAIL_DOMAIN")
            ?: throw IllegalArgumentException("Can't start without MAIL_DOMAIN")

    val tachikomaUrl = URI.create(
            System.getenv("TACHIKOMA_URL")
                    ?: throw IllegalArgumentException("Can't start without TACHIKOMA_URL")
    )

    val insecure = System.getenv("INSECURE")
            ?.toBoolean()
            ?: false

    Main(tachikomaUrl, mailDomain, insecure)
            .run()
}
