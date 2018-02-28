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
import com.sourceforgery.tachikoma.rest.RestBinder
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.get
import com.sourceforgery.tachikoma.logging.logger
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
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.Function

class WebServerStarter(
        private val serviceLocator: ServiceLocator
) {

    private fun startServerInBackground(): CompletableFuture<Void> {
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

        return serverBuilder
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
    }

    private fun startBackgroundWorkers() {
        serviceLocator.getService(JobWorker::class.java).work()
    }

    private fun initClientsInBackground() {
        listOf(
                MessageQueue::class.java,
                EbeanServer::class.java
        )
                // Yes, parallel stream is broken by design, but here it should work
                .parallelStream()
                .forEach({ serviceLocator.getService(it) })
    }

    fun start() {
        try {
            startDatabase()
            val server = startServerInBackground()
            startBackgroundWorkers()
            initClientsInBackground()
            server.join()
        } catch (e: Exception) {
            LOGGER.fatal(e, { "Failed to start server" })
            serviceLocator.shutdown()
            throw e
        }
    }

    private fun startDatabase() {
        val hk2RequestScope: HK2RequestContext = serviceLocator.get()
        serviceLocator
                .getServiceHandle(CreateUsers::class.java)
                .also { serviceHandle ->
                    hk2RequestScope.runInScope {
                        serviceHandle.service.createUsers()
                        serviceHandle.destroy()
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

    val serviceLocator = ServiceLocatorUtilities.bind(
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