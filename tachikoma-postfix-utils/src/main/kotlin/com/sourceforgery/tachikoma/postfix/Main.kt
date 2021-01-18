package com.sourceforgery.tachikoma.postfix

import com.linecorp.armeria.client.Clients
import com.sourceforgery.jersey.uribuilder.ensureGproto
import com.sourceforgery.jersey.uribuilder.withoutPassword
import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.incoming.IncomingEmailHandler
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpcKt
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpcKt
import com.sourceforgery.tachikoma.syslog.SyslogSniffer
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import java.time.Duration
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger

class Main
internal constructor(
    configuration: Configuration
) {

    private val tachikomaUrl = configuration.tachikomaUrl

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        LOGGER.error(exception) { "Coroutine uncaught exception" }
    }

    private val threadPool = ThreadPoolExecutor(
        0, 10,
        60L, TimeUnit.SECONDS,
        SynchronousQueue()
    )

    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob() + coroutineExceptionHandler)

    fun run() {
        LOGGER.info { "Connecting to ${tachikomaUrl.withoutPassword()}" }

        val apiToken = tachikomaUrl.userInfo
        val builder = Clients.builder(tachikomaUrl.withoutPassword().ensureGproto())
            .addHeader("x-apitoken", apiToken)
            .responseTimeout(Duration.ofDays(365))
            .writeTimeout(Duration.ofDays(365))
            .maxResponseLength(0)

        runCatching {
            MailSender(builder.build(MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub::class.java), scope)
                .start()
            val incomingEmail = IncomingEmailHandler(builder.build(MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub::class.java), scope)
            incomingEmail.start()
            SyslogSniffer(builder.build(MTADeliveryNotificationsGrpcKt.MTADeliveryNotificationsCoroutineStub::class.java))
                .blockingSniffer()
        }.onFailure {
            LOGGER.fatal(it) { "Syslog sniffer died. Dying with it." }
            exitProcess(1)
        }
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
