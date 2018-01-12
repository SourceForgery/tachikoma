package com.sourceforgery.tachikoma.mailer

import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import io.grpc.Channel
import io.grpc.stub.StreamObserver
import net.sf.expectit.ExpectBuilder
import net.sf.expectit.matcher.Matchers.contains
import net.sf.expectit.matcher.Matchers.regexp
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class Mailer(
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
                        .withInputs(smtpSocket.getInputStream())
                        .withOutput(smtpSocket.getOutputStream())
                        .withExceptionOnFailure()
                        .withTimeout(5, TimeUnit.SECONDS)
                        .build().use { expect ->
                    expect.interact()
                            .until(contains("220 "))
                    expect.sendLine("EHLO localhost")
                            .interact().until(contains("250 "))
                    expect.sendLine("MAIL FROM: ${value.from}")
                            .interact().until(contains("250 "))
                    expect.sendLine("RCPT TO: ${value.emailAddress}")
                            .interact().until(contains("250 "))
                    val queueId = expect.sendLine("DATA")
                            .send(value.body)
                            .sendLine(".")
                            .interact().until(regexp(Pattern.compile("^250 .* queued as (.*)$")))
                            .group(1)
                    expect.sendLine("QUIT")

                    return MTAQueuedNotification.newBuilder()
                            .setQueueId(queueId)
                            .setSuccess(true)
                            .build()
                }
            }
        } catch (e: Exception) {
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
            }
            start()
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