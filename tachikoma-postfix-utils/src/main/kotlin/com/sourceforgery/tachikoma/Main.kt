package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.incoming.IncomingEmail
import com.sourceforgery.tachikoma.mailer.MailSender
import com.sourceforgery.tachikoma.tracer.TraceMessageListener
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("172.17.0.1", 8070)
            .usePlaintext(true)
            .idleTimeout(365, TimeUnit.DAYS)
            .build()
    MailSender(channel).start()
    IncomingEmail(channel).start()
    TraceMessageListener(channel).startBlocking()
}