package com.sourceforgery.tachikoma.incoming

import com.sourceforgery.tachikoma.mta.MailAcceptanceResult
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.Writer
import java.net.Socket
import java.nio.charset.StandardCharsets

class LMTPServer(
    private val socket: Socket,
    private val param: suspend (String, String, String) -> MailAcceptanceResult
) {

    suspend fun receiveMail() {
        try {
            socket.use { lmtpSocket ->

                val xinput = InputStreamReader(lmtpSocket.getInputStream(), StandardCharsets.US_ASCII)
                val xoutput = OutputStreamWriter(lmtpSocket.getOutputStream(), StandardCharsets.US_ASCII)

                val input = BufferedReader(IoBuilder.forLogger(INCOMING_LOGGER.delegate).filter(xinput).setLevel(Level.TRACE).buildReader())
                val output = IoBuilder.forLogger(OUTGOING_LOGGER.delegate).filter(xoutput).setLevel(Level.TRACE).buildWriter()!!

                LOGGER.debug { "Welcoming incoming connection" }
                output.sendLine("220 localhost LMTP Tachikoma")

                input.assertRegex("LHLO (.*)")
                output.sendLine("250-localhost")
                output.sendLine("250-SIZE 10240000")
                output.sendLine("250 8BITMIME")

                val from = input
                    .assertRegex("MAIL FROM: *<(.*)>.*")
                    .groupValues[1]
                    .substringBeforeLast(' ')
                output.sendLine("250 OK")

                val to = input
                    .assertRegex("RCPT TO: *<(.*)>")
                    .groupValues[1]
                output.sendLine("250 OK")

                input.assertRegex("DATA")
                output.sendLine("354 End data with <CR><LF>.<CR><LF>")
                val data = input
                    .lineSequence()
                    .takeWhile {
                        it != "."
                    }
                    .joinToString(separator = "\r\n")
                val acceptanceResult = param(from, data, to)
                if (acceptanceResult.acceptanceStatus == MailAcceptanceResult.AcceptanceStatus.REJECTED) {
                    output.sendLine("550 nobody here with that email")
                } else {
                    output.sendLine("250 email queued")
                }

                input.assertRegex("QUIT")
                output.sendLine("221 Closing connection")
            }
        } catch (e: Exception) {
            LOGGER.error("", e)
        }
    }

    companion object {
        private val LOGGER = logger()
        private val INCOMING_LOGGER = logger("smtp.send.debug.incoming_in")
        private val OUTGOING_LOGGER = logger("smtp.send.debug.incoming_out")
    }
}

private fun BufferedReader.assertRegex(regex: String): MatchResult {
    val line = readLine()
    return Regex(regex).matchEntire(line)
        ?: throw RuntimeException("$line didn't match $regex")
}

private fun Writer.sendLine(line: String) {
    write(line)
    write("\r\n")
    flush()
}
