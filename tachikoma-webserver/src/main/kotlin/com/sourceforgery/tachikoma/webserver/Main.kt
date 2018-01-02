package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.startup.bindCommon
import io.grpc.BindableService
import org.glassfish.hk2.utilities.ServiceLocatorUtilities

@Suppress("unused")
fun main(vararg args: String) {

    val serviceLocator = bindCommon()
    ServiceLocatorUtilities.bind(serviceLocator, GrpcBinder(), RestBinder())

    val grpcServiceBuilder = GrpcServiceBuilder()
            .supportedSerializationFormats(GrpcSerializationFormats.values())!!
    for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
        grpcServiceBuilder.addService(grpcService)
    }

    val healthService = CorsServiceBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .allowRequestMethods(HttpMethod.GET)
            .build(object : HttpHealthCheckService() {})!!

    // Order matters!
    val serverBuilder = ServerBuilder()
            .serviceUnder("/health", healthService)

    for (restService in serviceLocator.getAllServices(RestService::class.java)) {
        serverBuilder.annotatedService(restService)
    }

    serverBuilder
            // Grpc must be last
            .serviceUnder("/", grpcServiceBuilder.build()!!)
            .build()
            .start()
            .join()
}