package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.hk2.RequestContext
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.startup.StartupBinder
import com.sourceforgery.tachikoma.webserver.hk2.RequestScopedService
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import io.grpc.BindableService
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.util.function.Function

@Suppress("unused")
fun main(vararg args: String) {

    val serviceLocator = ServiceLocatorUtilities.bind(
            CommonBinder(),
            StartupBinder(),
            RestBinder(),
            MqBinder(),
            GrpcBinder(),
            DatabaseBinder(),
            WebBinder()
    )!!

    val requestContext = serviceLocator.getService(RequestContext::class.java)

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
        serverBuilder.annotatedService("/", restService, Function { RequestScopedService(it, requestContext) })
    }

    val grpcService = grpcServiceBuilder.build()!!

    val requestContextGrpc = RequestScopedService(grpcService, requestContext)

    serverBuilder
            // Grpc must be last
            .serviceUnder("/", requestContextGrpc)
            .port(8070, SessionProtocol.HTTP)
            .build()
            .start()
            .join()
}