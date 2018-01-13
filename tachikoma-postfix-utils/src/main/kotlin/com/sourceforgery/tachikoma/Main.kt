package com.sourceforgery.tachikoma.mailer

import com.sourceforgery.tachikoma.tracer.TraceMessageListener
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val channel = ManagedChannelBuilder.forAddress("172.17.0.1", 8070)
            .usePlaintext(true)
            .idleTimeout(365, TimeUnit.DAYS)
            .build()
    MailSender(channel).start()
    TraceMessageListener(channel).startBlocking()
}