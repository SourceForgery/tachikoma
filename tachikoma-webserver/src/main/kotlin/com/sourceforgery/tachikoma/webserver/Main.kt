package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.DecoratingServiceFunction
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.startup.StartupBinder
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
    val hK2RequestContext = serviceLocator.getService(HK2RequestContext::class.java)

    val requestScoped = DecoratingServiceFunction<HttpRequest, HttpResponse> { delegate, ctx, req ->
        hK2RequestContext.runInScope {
            delegate.serve(ctx, req)
        }
    }

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
        serverBuilder.annotatedService("/", restService, Function { it.decorate(requestScoped) })
    }

    val grpcService = grpcServiceBuilder.build()!!
    grpcService.decorate(requestScoped)

    serverBuilder
            // Grpc must be last
            .serviceUnder("/", grpcService)
            .port(8070, SessionProtocol.HTTP)
            .build()
            .start()
            .join()
}