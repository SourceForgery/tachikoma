package com.sourceforgery.tachikoma.postfix

import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.incoming.IncomingEmail
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
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

class Main
internal constructor(
    private val configuration: Configuration
) {

    private val tachikomaUrl = addPort(configuration.tachikomaUrl)

    private fun withoutPassword(uri: URI): String {
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        return "${uri.scheme}://${uri.host}${uri.path ?: "/"}$query"
    }

    private fun addPort(uri: URI): URI {
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
        return URI.create("${uri.scheme}://${uri.userInfo}@${uri.host}:$port${uri.path ?: "/"}$query")
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
                    usePlaintext()
                } else {
                    useTransportSecurity()
                    sslContext(
                        GrpcSslContexts.forClient()
                            .also { ctx ->
                                if (configuration.insecure) {
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
    System.setOut(IoBuilder.forLogger("System.sout").setLevel(Level.WARN).buildPrintStream())
    System.setErr(IoBuilder.forLogger("System.serr").setLevel(Level.ERROR).buildPrintStream())

    val configuration = Configuration()
    Main(configuration)
        .run()
}
