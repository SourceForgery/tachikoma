package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.incoming.IncomingEmail
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.tracer.TraceMessageListener
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.net.URI
import java.util.concurrent.TimeUnit

private val APITOKEN_HEADER = Metadata.Key.of("x-apitoken", Metadata.ASCII_STRING_MARSHALLER)

fun main(args: Array<String>) {

    val mailDomain = System.getenv("MAIL_DOMAIN")
            ?: throw IllegalArgumentException("Can't start without MAIL_DOMAIN")

    val tachikomaUrl = URI.create(
            System.getenv("TACHIKOMA_URL")
                    ?: "http://$mailDomain:oodua5yai9Pah5ook3wah4hahqu4IeK0iung8ou5Cho4Doonee@172.17.0.1:8070"
    )
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
            .usePlaintext(plaintext)
            .idleTimeout(365, TimeUnit.DAYS)
            .build()
    MailSender(channel).start()
    IncomingEmail(channel).start()
    TraceMessageListener(channel).startBlocking()
}