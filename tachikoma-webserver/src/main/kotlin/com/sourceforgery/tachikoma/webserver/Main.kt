package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.Service
import com.linecorp.armeria.server.cors.CorsServiceBuilder
import com.linecorp.armeria.server.grpc.GrpcServiceBuilder
import com.linecorp.armeria.server.healthcheck.HttpHealthCheckService
import com.sourceforgery.rest.RestBinder
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.hk2.get
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
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.io.File
import java.time.Duration
import java.util.function.Function

@Suppress("unused")
fun main(vararg args: String) {
    InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE)

    val serviceLocator = ServiceLocatorUtilities.bind(
            CommonBinder(),
            StartupBinder(),
            RestBinder(),
            MqBinder(),
            GrpcBinder(),
            DatabaseBinder(),
            WebBinder()
    )!!

    val requestScoped: HttpRequestScopedDecorator = serviceLocator.get()

    val healthService = CorsServiceBuilder
            .forAnyOrigin()
            .allowNullOrigin()
            .allowCredentials()
            .allowRequestMethods(HttpMethod.GET)
            .build(object : HttpHealthCheckService() {})

    // Order matters!
    val serverBuilder = ServerBuilder()
            .serviceUnder("/health", healthService)
    val exceptionHandler: RestExceptionHandlerFunction = serviceLocator.get()

    val restDecoratorFunction = Function<Service<HttpRequest, HttpResponse>, Service<HttpRequest, HttpResponse>> { it.decorate(requestScoped) }
    for (restService in serviceLocator.getAllServices(RestService::class.java)) {
        serverBuilder.annotatedService("/", restService, restDecoratorFunction, exceptionHandler)
    }

    val exceptionInterceptor: GrpcExceptionInterceptor = serviceLocator.get()
    val webServerConfig: WebServerConfig = serviceLocator.get()

    val grpcServiceBuilder = GrpcServiceBuilder().supportedSerializationFormats(GrpcSerializationFormats.values())
    for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
        grpcServiceBuilder.addService(ServerInterceptors.intercept(grpcService, exceptionInterceptor))
    }
    val grpcService = grpcServiceBuilder.build()

    serviceLocator.getService(JobWorker::class.java).work()

    val server = serverBuilder
            // Grpc must be last
            .decorator(Function { it.decorate(requestScoped) })
            .serviceUnder("/", grpcService)
            .apply {
                if (webServerConfig.sslCertChainFile.isNotEmpty() && webServerConfig.sslCertKeyFile.isNotEmpty()) {
                    sslContext(SessionProtocol.HTTPS, File(webServerConfig.sslCertChainFile), File(webServerConfig.sslCertKeyFile))
                    port(8443, SessionProtocol.HTTPS)
                } else {
                    port(8070, SessionProtocol.HTTP)
                }
            }
            .defaultRequestTimeout(Duration.ofDays(365))
            .build()
            .start()

    listOf(
            MessageQueue::class.java,
            EbeanServer::class.java
    )
            // Yes, parallel stream is broken by design, but here it should work
            .parallelStream()
            .forEach({ serviceLocator.getService(it) })

    server.join()
}