package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.tachikoma.mta.grpc.MTADeliveryService
import com.sourceforgery.tachikoma.mta.grpc.MTAEmailQueueService

fun main() {
    val grpcService = GrpcServiceBuilder()
            .addService(MTADeliveryService())
            .addService(MTAEmailQueueService())
            .supportedSerializationFormats(GrpcSerializationFormats.values())
            .build()!!


    val healthService = CorsServiceBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .build(object : HttpHealthCheckService() {})


    // Order matters!
    val server = ServerBuilder()
            .serviceUnder("/health", healthService)
            .serviceUnder("/", grpcService)
            .build()!!

    server
            .start()
            .join()

}