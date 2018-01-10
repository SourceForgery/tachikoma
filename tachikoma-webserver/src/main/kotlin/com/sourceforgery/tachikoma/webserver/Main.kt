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
import com.sourceforgery.tachikoma.mq.JobWorker
import com.sourceforgery.tachikoma.mq.MessageQueue
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.startup.StartupBinder
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import io.ebean.EbeanServer
import io.grpc.BindableService
import io.grpc.ServerInterceptors
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.time.Duration
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

    listOf(
            MessageQueue::class.java,
            EbeanServer::class.java
    )
            // Yes, parallal stream is broken by design, but here it should work
            .parallelStream()
            .forEach({ serviceLocator.getService(it) })

    val requestScoped = serviceLocator.getService(HttpRequestScopedDecorator::class.java)

    val healthService = CorsServiceBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .allowRequestMethods(HttpMethod.GET)
            .build(object : HttpHealthCheckService() {})!!


    // Order matters!
    val serverBuilder = ServerBuilder()
            .serviceUnder("/health", healthService)
    val exceptionHandler = serviceLocator.getService(RestExceptionHandlerFunction::class.java)
    for (restService in serviceLocator.getAllServices(RestService::class.java)) {
        serverBuilder.annotatedService("/", restService, exceptionHandler)
    }

    val exceptionInterceptor = serviceLocator.getService(GrpcExceptionInterceptor::class.java)

    val grpcServiceBuilder = GrpcServiceBuilder().supportedSerializationFormats(GrpcSerializationFormats.values())!!
    for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
        grpcServiceBuilder.addService(ServerInterceptors.intercept(grpcService, exceptionInterceptor))
    }
    val grpcService = grpcServiceBuilder.build()!!

    serviceLocator.getService(JobWorker::class.java).work()

    serverBuilder
            // Grpc must be last
            .decorator(Function { it.decorate(requestScoped) })
            .serviceUnder("/", grpcService)
            .port(8070, SessionProtocol.HTTP)
            .defaultRequestTimeout(Duration.ofDays(365))
            .build()
            .start()
            .join()
}