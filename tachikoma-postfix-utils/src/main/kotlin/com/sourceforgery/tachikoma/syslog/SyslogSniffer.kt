package com.sourceforgery.tachikoma.syslog

import com.google.common.base.Splitter
import com.sourceforgery.tachikoma.mta.DeliveryNotification
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpcKt
import com.squareup.tape.FileObjectQueue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class SyslogSniffer(
    private val stub: MTADeliveryNotificationsGrpcKt.MTADeliveryNotificationsCoroutineStub
) {
    private val notificationQueue = LinkedBlockingQueue<DeliveryNotification>()

    private val tape = FileObjectQueue<DeliveryNotification>(
        File("/var/spool/postfix/tachikoma/notification_queue"),
        DeliveryNotificationConverter
    )

    @OptIn(DelicateCoroutinesApi::class)
    private val deliverer = GlobalScope.launch(
        context = delivererDispatcher,
        start = CoroutineStart.LAZY
    ) {
        while (true) {
            runDeliverer()
            saveAll()
        }
    }

    private fun saveAll() {
        synchronized(tape) {
            generateSequence(notificationQueue.take()) {
                notificationQueue.poll()
            }.forEach {
                tape.add(it)
            }
        }
    }

    private suspend fun runDeliverer() {
        runCatching {
            for (notification in generateSequence { synchronized(tape) { tape.peek() } }) {
                runCatching {
                    stub.setDeliveryStatus(notification)
                }.recover {
                    delay(1000)
                    stub.setDeliveryStatus(notification)
                }.onSuccess {
                    synchronized(tape) {
                        tape.remove()
                    }
                }.onFailure {
                    LOGGER.error { "Failed to send notification $notification because of '${it.message}'. Will retry!" }
                }.getOrThrow()
            }
            LOGGER.debug { "Successfully delivered all notifications in queue" }
        }.onFailure {
            LOGGER.debug(it) { "Failed to process whole queue." }
            synchronized(tape) {
                if (tape.size() > 20) {
                    LOGGER.error { "DeliveryNotification queue is filling up. ${tape.size()} messages in queue now." }
                }
            }
            delay(30000)
        }
    }

    fun blockingSniffer() {
        // This only starts if it isn't already running.
        deliverer.start()
        LOGGER.info("Started logging")
        val file = File("/opt/maillog_pipe")
        try {
            while (!Thread.interrupted()) {
                @Suppress("BlockingMethodInNonBlockingContext")
                runBlocking {
                    RandomAccessFile(file, "r").use { pipe ->
                        val line = pipe.readLine()
                            ?: error("End of file? This is a socket, so that should not happen!")
                        val notification = parseLine(line)

                        if (notification != null) {
                            LOGGER.debug { ">>>>$line<<<<" }
                            notificationQueue.add(notification)
                        }
                        // >>>>Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)<<<<
                    }
                }
            }
        } finally {
            LOGGER.info("Finished reading file")
        }
    }

    companion object {
        private val delivererDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        internal val LOGGER = logger()
    }
}

internal fun parseLine(line: String): DeliveryNotification? {
    val split = syslogLineSplitter.splitToList(line)
    SyslogSniffer.LOGGER.trace { "[[[[$line]]]] (${split.size})" }
    return if (split.size == 3 && !line.contains("postfix/lmtp")) {
        val (_, queueId, rest) = split
        val map = try {
            splitLineToMap(rest)
        } catch (e: Exception) {
            SyslogSniffer.LOGGER.error(e) { "Failed to parse line $line" }
            return null
        }

        map["dsn"]?.let { dsn ->
            map["to"]?.let { originalRecipient ->
                map["status"]?.let { status ->
                    DeliveryNotification.newBuilder()
                        .setDiagnoseText(status)
                        .setReason(status.substringBefore(' '))
                        .setQueueId(queueId)
                        .setStatus(dsn)
                        .setOriginalRecipient(originalRecipient.trim('<', '>'))
                        .build()
                }
            }
        }
    } else {
        null
    }
}

@Suppress("UnstableApiUsage")
private fun splitLineToMap(rest: String): Map<String, String> =
    lineSplitter.splitToList(rest)
        .asSequence()
        // Must exist and must NOT be first character
        .filter { it.indexOf('=') > 0 }
        .associate {
            it.substringBefore("=", "") to it.substringAfter("=", "")
        }

private val syslogLineSplitter = Splitter
    .on(": ")
    .limit(3)
    .trimResults()

@Suppress("UnstableApiUsage")
private val lineSplitter = Splitter
    .on(", ")
    .trimResults()

private object DeliveryNotificationConverter : FileObjectQueue.Converter<DeliveryNotification> {
    override fun from(bytes: ByteArray): DeliveryNotification =
        DeliveryNotification.parseFrom(bytes)

    override fun toStream(o: DeliveryNotification, bytes: OutputStream) =
        o.writeTo(bytes)
}
