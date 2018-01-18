@file:Suppress("UnusedImport")

package com.sourceforgery.tachikoma.syslog

import com.sourceforgery.tachikoma.logging.logger
import java.io.File
import java.io.RandomAccessFile

class Syslogger {
    fun blockingSniffer() {
        LOGGER.info("Started logging")
        val file = File("/opt/maillog_pipe")
        try {
            while (!Thread.interrupted()) {
                RandomAccessFile(file, "r").use { pipe ->
                    val line = pipe.readLine()!!
                    // >>>>Jan 18 22:55:46 1c7326acd8e5 postfix/smtp[249]: 2D61E2A03: to=<test@example.com>, relay=none, delay=30, delays=0.01/0/30/0, dsn=4.4.1, status=deferred (connect to example.com[93.184.216.34]:25: Connection timed out)<<<<


                    LOGGER.error { ">>>>$line<<<<" }
                }
            }
        } finally {
            LOGGER.info("Finished reading file")
        }
    }

    companion object {
        val LOGGER = logger()
    }
}