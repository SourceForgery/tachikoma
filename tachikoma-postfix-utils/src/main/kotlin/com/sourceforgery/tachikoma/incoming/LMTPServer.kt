package com.sourceforgery.tachikoma.incoming

import com.sourceforgery.tachikoma.expectit.emptyBuffer
import com.sourceforgery.tachikoma.expectit.expectNoQuit
import com.sourceforgery.tachikoma.expectit.regexpLine
import com.sourceforgery.tachikoma.mailer.MailSender
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder
import java.net.Socket
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.util.concurrent.TimeUnit

class LMTPServer(
        private val socket: Socket,
        private val param: (String, ByteArray, String) -> Unit
) : Runnable {

    private var from: String? = null
    private var to: String? = null

    override fun run() {
        try {
            socket.use { lmtpSocket ->
                ExpectBuilder()
                        .withEchoOutput(System.err)
                        .withEchoInput(System.err)
                        .withInputs(lmtpSocket.getInputStream())
                        .withOutput(lmtpSocket.getOutputStream())
                        .withLineSeparator("\r\n")
                        .withExceptionOnFailure()
                        .withTimeout(180, TimeUnit.SECONDS)
                        .build().use { expect ->
                    expect.sendLine("220 localhost LMTP Tachikoma")

                    expect.expectNoQuit("^LHLO (.*)")
                    expect.emptyBuffer()
                    expect.sendLine("250-localhost")
                    expect.sendLine("250-SIZE 10240000")
                    expect.sendLine("250 8BITMIME")

                    expect.interact()
                            .`when`(regexpLine("^MAIL FROM: (.*)$")).then({ from = it.group(1) })
                            .`when`(regexpLine("^RCPT TO: (.*)$")).then({ to = it.group(1) })
                            .`when`(regexpLine("^DATA$")).then({ readBodyAndSend(expect) })
                            .until(regexpLine("^QUIT$"))
                }
            }
        } catch (e: Exception) {
            MailSender.LOGGER.error("", e)
        }
    }

    fun readBodyAndSend(expect: Expect) {
        val to = this.to ?: throw NullPointerException("to address is not set")
        val from = from ?: throw NullPointerException("from address is not set")
        expect.emptyBuffer()
        expect.sendLine("354 End data with <CR><LF>.<CR><LF>")
        val bytes = expect.expect(regexpLine("^\\.$")).before.toByteArray(ISO_8859_1)
        try {
            param(from, bytes, to)
            expect.sendLine("250 email queued")
        } catch (e: Exception) {
            expect.sendLine("500 Failed")
            throw e
        }
    }
}
