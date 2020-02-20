package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.HttpService
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.cors.CorsService
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.healthcheck.HealthCheckService
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.getValue
import com.sourceforgery.tachikoma.mq.JobWorker
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.rest.RestBinder
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.startup.StartupBinder
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.hk2.WebBinder
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import io.grpc.BindableService
import io.grpc.ServerInterceptors
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.Function
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities

class WebServerStarter(
    private val serviceLocator: ServiceLocator
) {

    private fun startServerInBackground(): CompletableFuture<Void> {
        val requestScoped: HttpRequestScopedDecorator by serviceLocator

        val healthService = CorsService
            .builderForAnyOrigin()
            .allowCredentials()
            .allowRequestMethods(HttpMethod.GET)
            .build(HealthCheckService.of())

        // Order matters!
        val serverBuilder = Server.builder()
            .serviceUnder("/health", healthService)
            .accessLogWriter(AccessLogWriter.combined(), true)
        val exceptionHandler: RestExceptionHandlerFunction by serviceLocator

        val restDecoratorFunction = Function<HttpService, HttpService> { it.decorate(requestScoped) }
        for (restService in serviceLocator.getAllServices(RestService::class.java)) {
            serverBuilder.annotatedService("/", restService, restDecoratorFunction, exceptionHandler)
        }

        val exceptionInterceptor: GrpcExceptionInterceptor by serviceLocator
        val webServerConfig: WebServerConfig by serviceLocator

        val grpcServiceBuilder = GrpcService.builder().supportedSerializationFormats(GrpcSerializationFormats.values())
        for (grpcService in serviceLocator.getAllServices(BindableService::class.java)) {
            grpcServiceBuilder.addService(ServerInterceptors.intercept(grpcService, exceptionInterceptor))
        }
        val grpcService = grpcServiceBuilder.build()

        return serverBuilder
            // Grpc must be last
            .decorator(requestScoped)
            .serviceUnder("/", grpcService)
            .apply {
                if (webServerConfig.sslCertChainFile.isNotEmpty() && webServerConfig.sslCertKeyFile.isNotEmpty()) {
                    tls(File(webServerConfig.sslCertChainFile), File(webServerConfig.sslCertKeyFile))
                    port(8443, SessionProtocol.HTTPS)
                } else {
                    port(8070, SessionProtocol.HTTP)
                    LOGGER.warn { "Unsubscribe may not work properly as rfc8058 REQUIRES https" }
                }
            }
            .requestTimeout(Duration.ofDays(365))
            .build()
            .start()
    }

    private fun startBackgroundWorkers() {
        serviceLocator.getService(JobWorker::class.java).work()
    }

    fun start() {
        try {
            startDatabase()
            val server = startServerInBackground()
            startBackgroundWorkers()
            server.join()
        } catch (e: Exception) {
            LOGGER.fatal(e) { "Failed to start server" }
            serviceLocator.shutdown()
            throw e
        }
    }

    private fun startDatabase() {
        val hk2RequestScope: HK2RequestContext by serviceLocator
        serviceLocator
            .getServiceHandle(CreateUsers::class.java)
            .also { serviceHandle ->
                hk2RequestScope.runInNewScope {
                    serviceHandle.service.createUsers()
                    serviceHandle.close()
                }
            }
    }

    companion object {
        private val LOGGER = logger()
    }
}

@Suppress("unused")
fun main(vararg args: String) {
    InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE)
    System.setOut(IoBuilder.forLogger("System.sout").setLevel(Level.WARN).buildPrintStream())
    System.setErr(IoBuilder.forLogger("System.serr").setLevel(Level.ERROR).buildPrintStream())

    val serviceLocator = ServiceLocatorUtilities.bind(
        "Webserver",
        CommonBinder(),
        StartupBinder(),
        RestBinder(),
        MqBinder(),
        GrpcBinder(),
        DatabaseBinder(),
        WebBinder()
    )!!
    WebServerStarter(serviceLocator).start()
}