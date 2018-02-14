package com.sourceforgery.tachikoma.mailer

import com.sourceforgery.tachikoma.expectit.emptyBuffer
import com.sourceforgery.tachikoma.expectit.expectNoSmtpError
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import io.grpc.Channel
import io.grpc.stub.StreamObserver
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.matcher.Matchers.regexp
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MailSender(
        channel: Channel
) {
    private lateinit var response: StreamObserver<MTAQueuedNotification>
    private val stub = MTAEmailQueueGrpc.newStub(channel)

    fun start() {
        response = stub.getEmails(fromServerStreamObserver)
        LOGGER.info { "Connecting. Trying to listen for emails" }
    }

    fun sendEmail(emailMessage: EmailMessage): MTAQueuedNotification {
        LOGGER.info { "Got email: ${emailMessage.emailId}" }

        try {
            Socket("localhost", 25).use { smtpSocket ->
                val os = IoBuilder.forLogger("smtp.debug").setLevel(Level.TRACE).buildPrintWriter()
                ExpectBuilder()
                        .withEchoOutput(os)
                        .withEchoInput(os)
                        .withInputs(smtpSocket.getInputStream())
                        .withOutput(smtpSocket.getOutputStream())
                        .withLineSeparator("\r\n")
                        .withExceptionOnFailure()
                        .withTimeout(180, TimeUnit.SECONDS)
                        .build().use { expect ->
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
                    val queueId = expect.send(emailMessage.body)
                            .sendLine(".")
                            .expectNoSmtpError("^250 .* queued as (.*)$")
                            .group(1)
                    expect.sendLine("QUIT")

                    LOGGER.info { "Successfully send email: ${emailMessage.emailId}" }
                    return MTAQueuedNotification.newBuilder()
                            .setQueueId(queueId)
                            .setEmailId(emailMessage.emailId)
                            .setSuccess(true)
                            .build()
                }
            }
        } catch (e: Exception) {
            LOGGER.error("", e)
            return MTAQueuedNotification.newBuilder()
                    .setEmailId(emailMessage.emailId)
                    .setSuccess(false)
                    .build()
        }

        // EHLO <domain> (localhost)
        // MAIL FROM: foobar@domain
        // RCPT TO: receiver@domain
        // DATA
        //... data
        // .
        // quit
    }

    private val fromServerStreamObserver = object : StreamObserver<EmailMessage> {
        override fun onError(t: Throwable) {
            if (!Thread.currentThread().isInterrupted) {
                LOGGER.warn { "Got error from gRPC server with message: ${t.message}" }
                Thread.sleep(1000)
                start()
            }
        }

        override fun onCompleted() {
            response.onCompleted()
            Thread.sleep(1000)
            start()
        }

        override fun onNext(value: EmailMessage) {
            val status = sendEmail(value)
            response.onNext(status)
            LOGGER.info { "Send email: ${value.emailId} with status ${status.success} and queueId ${status.queueId}" }
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
