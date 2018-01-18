@file:Suppress("UnusedImport")

package com.sourceforgery.tachikoma.syslog

import com.sourceforgery.tachikoma.logging.logger
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class Syslogger {
    fun blockingSniffer() {
        LOGGER.info("Started logging")
        val file = File("/opt/maillog_pipe")
        try {
            BufferedReader(FileReader(file)).use { pipe ->
                LOGGER.info("Actually reading file")
                while (true) {
                    val line = pipe.readLine()!!
                    LOGGER.error { ">>>> $line <<<<" }
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