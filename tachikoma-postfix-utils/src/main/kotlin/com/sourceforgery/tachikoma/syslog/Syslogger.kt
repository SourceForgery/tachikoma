@file:Suppress("UnusedImport")

package com.sourceforgery.tachikoma.syslog

import com.sourceforgery.tachikoma.mta.DeliveryNotification
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpc
import io.grpc.Channel
import java.io.File
import java.io.RandomAccessFile
import org.apache.logging.log4j.kotlin.logger

class Syslogger(grpcChannel: Channel) {

    private val stub = MTADeliveryNotificationsGrpc.newBlockingStub(grpcChannel)

    fun blockingSniffer() {
        LOGGER.info("Started logging")
        val file = File("/opt/maillog_pipe")
        try {
            while (!Thread.interrupted()) {
                RandomAccessFile(file, "r").use { pipe ->
                    val line = pipe.readLine()!!
                    val split = line.split(": ", limit = 3)
                    if (split.size == 3) {
                        val (_, queueId, rest) = split
                        val map = parseLine(rest)

                        map["dsn"]?.let { dsn ->
                            map["to"]?.let { originalRecipient ->
                                map["status"]?.let { status ->
                                    val notification = DeliveryNotification.newBuilder()
                                        .setDiagnoseText(status)
                                        .setReason(status.substringBefore(' '))
                                        .setQueueId(queueId)
                                        .setStatus(dsn)
                                        .setOriginalRecipient(originalRecipient)
                                        .build()
                                    stub.setDeliveryStatus(notification)
                                    ""
                                }
                            }
                        }
                    } else {
                        null
                    }
                        ?: LOGGER.warn { ">>>>$line<<<<" }
                    // >>>>Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)<<<<
                }
            }
        } finally {
            LOGGER.info("Finished reading file")
        }
    }

    private fun parseLine(rest: String): Map<String, String> {
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