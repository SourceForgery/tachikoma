package com.sourceforgery.tachikoma.mailer

import com.sourceforgery.tachikoma.expectit.emptyBuffer
import com.sourceforgery.tachikoma.expectit.expectNoSmtpError
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpcKt
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sf.expectit.Expect
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.matcher.Matchers.regexp
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.system.exitProcess

class MailSender(
    private val stub: MTAEmailQueueGrpcKt.MTAEmailQueueCoroutineStub,
    private val scope: CoroutineScope
) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()
    private val channel = Channel<MTAQueuedNotification>()

    override fun close() {
        channel.close()
        executor.shutdown()
    }

    fun start() {
        scope.launch {
            // Keep-alive
            while (true) {
                try {
                    channel.offer(MTAQueuedNotification.getDefaultInstance())
                    delay(30000)
                } catch (e: Exception) {
                    LOGGER.warn(e) { "Keep-alive failed" }
                }
            }
        }
        scope.launch {
            while (true) {
                LOGGER.info { "Connecting. Trying to listen for emails" }
                try {
                    stub.getEmails(channel.receiveAsFlow())
                        .collect {
                            val status = sendEmail(it)
                            LOGGER.info { "Queued email: ${it.emailId} for delivery with status ${status.success} and queueId ${status.queueId}" }
                            channel.offer(status)
                        }
                } catch (e: Exception) {
                    LOGGER.warn { "Got error from gRPC server with message: ${e.message}" }
                    delay(1000)
                }
            }
        }.invokeOnCompletion {
            LOGGER.fatal { "MailSender died. Killing process" }
            exitProcess(1)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendEmail(emailMessage: EmailMessage): MTAQueuedNotification {
        LOGGER.info { "Got email: ${emailMessage.emailId}" }

        val builder = MTAQueuedNotification.newBuilder()
            .setEmailId(emailMessage.emailId)
        val success = try {
            withContext(executor.asCoroutineDispatcher()) {
                Socket("localhost", 25).use { smtpSocket ->
                    IoBuilder.forLogger("smtp.debug")
                        .setLevel(Level.TRACE)
                        .buildPrintWriter()!!
                        .use { os ->
                            createExpect(os, smtpSocket).use { expect ->
                                val queueId = ssmtpSendEmail(expect, emailMessage)
                                builder.queueId = queueId
                                LOGGER.debug { "Successfully queued email: ${emailMessage.emailId} with QueueId: $queueId" }
                                true
                            }
                        }
                }
            }
        } catch (e: Exception) {
            LOGGER.error(e) { "Failed to send message" }
            false
        }
        return builder.setSuccess(success)
            .build()
    }

    private fun createExpect(os: PrintWriter, smtpSocket: Socket): Expect = ExpectBuilder()
        .withEchoOutput(os)
        .withEchoInput(os)
        .withInputs(smtpSocket.getInputStream())
        .withOutput(smtpSocket.getOutputStream())
        .withLineSeparator("\r\n")
        .withExceptionOnFailure()
        .withExecutor(executor)
        .withTimeout(180, TimeUnit.SECONDS)
        .build()

    private fun ssmtpSendEmail(expect: Expect, emailMessage: EmailMessage): String {
        expect.expectNoSmtpError("^220 ")
        expect.emptyBuffer()
        expect.sendLine("EHLO localhost")
            .expectNoSmtpError("^250 ")
        expect.emptyBuffer()
        expect.sendLine("MAIL FROM: ${emailMessage.from}")
            .expectNoSmtpError("^250 ")
        expect.sendLine("RCPT TO: ${emailMessage.emailAddress}")
            .expectNoSmtpError("^250 ")
        expect.emptyBuffer()
        emailMessage.bccList.forEach {
            expect.sendLine("RCPT TO: $it")
                .expectNoSmtpError("^250 ")
            expect.emptyBuffer()
        }
        expect.sendLine("DATA")
            .expectNoSmtpError("^354 ")
        expect.expect(regexp(Pattern.compile(".*", Pattern.DOTALL)))
        // Every line that starts with a dot must have it 'escaped' with an extra dot
        val preProcessedBody = emailMessage.body
            .replace(Regex("\n\\."), "..")
            .replace(Regex("^\\."), "..")
        val queueId = expect.send(preProcessedBody)
            .sendLine(".")
            .expectNoSmtpError("^250 .* queued as (.*)$")
            .group(1)!!
        expect.sendLine("QUIT")

        return queueId

        // EHLO <domain> (localhost)
        // MAIL FROM: foobar@domain
        // RCPT TO: receiver@domain
        // DATA
        // ... data
        // .
        // quit
    }

    companion object {
        private val LOGGER = logger()
    }
}
