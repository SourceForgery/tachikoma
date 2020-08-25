package com.sourceforgery.tachikoma.postfix

import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.addPort
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.incoming.IncomingEmailHandler
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpc
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.syslog.Syslogger
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import java.time.Duration
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger

class Main
internal constructor(
    private val configuration: Configuration
) {

    private val tachikomaUrl = configuration.tachikomaUrl.addPort()

    fun run() {
        LOGGER.info { "Connecting to ${tachikomaUrl.withoutPassword()}" }

        val builder = Clients.builder(tachikomaUrl.withoutPassword())
            .addHeader("x-apitoken", tachikomaUrl.userInfo)
            .responseTimeout(Duration.ofDays(365))
            .writeTimeout(Duration.ofDays(365))

        MailSender(builder.build(MTAEmailQueueGrpc.MTAEmailQueueStub::class.java))
            .start()
        val incomingEmail = IncomingEmailHandler(builder.build(MTAEmailQueueGrpc.MTAEmailQueueBlockingStub::class.java))
        incomingEmail.start()
        Syslogger(builder.build(MTADeliveryNotificationsGrpc.MTADeliveryNotificationsBlockingStub::class.java))
            .blockingSniffer()
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
