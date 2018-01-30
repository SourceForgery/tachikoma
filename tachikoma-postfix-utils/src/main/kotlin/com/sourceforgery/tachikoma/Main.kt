package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.incoming.IncomingEmail
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.syslog.Syslogger
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.net.URI
import java.util.concurrent.TimeUnit

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

class Main(
        private val tachikomaUrl: URI,
        private val mailDomain: String
) {

    private fun withoutPassword(uri: URI): String {
        val query = uri.rawQuery?.let { "?$it" } ?: ""
        return "${uri.scheme}://${uri.host}${uri.path ?: "/"}$query"
    }

    fun run() {
        LOGGER.info { "Connecting to ${withoutPassword(tachikomaUrl)}" }

        val plaintext = tachikomaUrl.scheme == "http"
        val port: Int =
                if (tachikomaUrl.port == -1) {
                    when (tachikomaUrl.scheme) {
                        "http" -> 80
                        "https" -> 443
                        else -> throw IllegalArgumentException("Unknown proto's default port is unknown")
                    }
                } else {
                    tachikomaUrl.port
                }

        val metadataAuth = Metadata()
        metadataAuth.put(APITOKEN_HEADER, tachikomaUrl.userInfo)

        val channel = ManagedChannelBuilder.forAddress(tachikomaUrl.host, port)
                .intercept(MetadataUtils.newAttachHeadersInterceptor(metadataAuth))
                .apply {
                    if (plaintext) {
                        usePlaintext(true)
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

    val mailDomain = System.getenv("MAIL_DOMAIN")
            ?: throw IllegalArgumentException("Can't start without MAIL_DOMAIN")

    val tachikomaUrl = URI.create(
            System.getenv("TACHIKOMA_URL")
                    ?: "http://$mailDomain:oodua5yai9Pah5ook3wah4hahqu4IeK0iung8ou5Cho4Doonee@172.17.0.1:8070"
    )
    Main(tachikomaUrl, mailDomain)
            .run()
}
