@file:Suppress("UnusedImport")

package com.sourceforgery.tachikoma.syslog

import com.sourceforgery.tachikoma.mta.DeliveryNotification
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpc
import java.io.File
import java.io.RandomAccessFile
import org.apache.logging.log4j.kotlin.logger

class Syslogger(
    private val stub: MTADeliveryNotificationsGrpc.MTADeliveryNotificationsBlockingStub
) {

    fun blockingSniffer() {
        LOGGER.info("Started logging")
        val file = File("/opt/maillog_pipe")
        try {
            while (!Thread.interrupted()) {
                RandomAccessFile(file, "r").use { pipe ->
                    val line = pipe.readLine()!!
                    val notification = parseLine(line)
                    if (notification != null) {
                        stub.setDeliveryStatus(notification)
                        LOGGER.debug { ">>>>$line<<<<" }
                    }
                    // >>>>Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)<<<<
                }
            }
        } finally {
            LOGGER.info("Finished reading file")
        }
    }

    internal fun parseLine(line: String): DeliveryNotification? {
        val split = line.split(": ", limit = 3)
        return if (split.size == 3 && !line.contains("postfix/lmtp")) {
            val (_, queueId, rest) = split
            val map = splitLine(rest)

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

    private fun splitLine(rest: String): Map<String, String> {
        val map = rest.split(", ")
            .asSequence()
            .map {
                it.substringBefore("=", "") to it.substringAfter("=", "")
            }
            .filter {
                it.first.isNotBlank()
            }
            .toMap()
        return map
    }

    companion object {
        val LOGGER = logger()
    }
}
