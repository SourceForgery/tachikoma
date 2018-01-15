package com.sourceforgery.tachikoma.mailer

import com.google.protobuf.util.JsonFormat
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
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MailSender(
        channel: Channel
) {
    private lateinit var response: StreamObserver<MTAQueuedNotification>
    private val stub = MTAEmailQueueGrpc.newStub(channel)!!

    fun start() {
        response = stub.getEmails(fromServerStreamObserver)!!
    }

    fun sendEmail(value: EmailMessage): MTAQueuedNotification {
        LOGGER.debug { "Got email: " + JsonFormat.printer().print(value) }

        try {
            Socket("localhost", 25).use { smtpSocket ->
                ExpectBuilder()
                        .withEchoOutput(System.err)
                        .withEchoInput(System.err)
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
                    expect.sendLine("MAIL FROM: ${value.from}")
                            .expectNoSmtpError("^250 ")
                    expect.sendLine("RCPT TO: ${value.emailAddress}")
                            .expectNoSmtpError("^250 ")
                    expect.emptyBuffer()
                    expect.sendLine("DATA")
                            .expectNoSmtpError("^354 ")
                    expect.expect(regexp(Pattern.compile(".*", Pattern.DOTALL)))
                    val queueId = expect.send(value.body)
                            .sendLine(".")
                            .expectNoSmtpError("^250 .* queued as (.*)$")
                            .group(1)
                    expect.sendLine("QUIT")

                    return MTAQueuedNotification.newBuilder()
                            .setQueueId(queueId)
                            .setSuccess(true)
                            .build()
                }
            }
        } catch (e: Exception) {
            LOGGER.error("", e)
            return MTAQueuedNotification.newBuilder()
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
                LOGGER.warn(t, { "Got error from gRPC server" })
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
            System.err.println("Got email: " + JsonFormat.printer().print(value))
        }
    }

    companion object {
        val LOGGER = logger()
    }
}
