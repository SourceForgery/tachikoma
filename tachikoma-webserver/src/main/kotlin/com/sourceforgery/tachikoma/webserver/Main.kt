package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.healthcheck.HealthCheckService
import com.linecorp.armeria.server.healthcheck.HealthChecker
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.sourceforgery.tachikoma.commonModule
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.databaseModule
import com.sourceforgery.tachikoma.grpcModule
import com.sourceforgery.tachikoma.kodein.withNewDatabaseSessionScope
import com.sourceforgery.tachikoma.mq.JobWorker
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.mqModule
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.rest.restModule
import com.sourceforgery.tachikoma.startup.startupModule
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.hk2.webModule
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import io.ebean.Database
import io.grpc.BindableService
import io.grpc.ServerInterceptors
import io.netty.channel.ChannelOption
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

class WebServerStarter(override val di: DI) : DIAware {
    private val exceptionHandler: RestExceptionHandlerFunction by instance()
    private val restServices: List<RestService> by allInstances()
    private val grpcServices: List<BindableService> by allInstances()
    private val exceptionInterceptor: GrpcExceptionInterceptor by instance()
    private val webServerConfig: WebServerConfig by instance()
    private val jobWorker: JobWorker by instance()
    private val createUsers: CreateUsers by instance()
    private val mqSequenceFactory: MQSequenceFactory by instance()
    private val database: Database by instance()
    private val requestScoped: HttpRequestScopedDecorator by instance()

    private fun startServerInBackground(): CompletableFuture<Void> {

        val healthService = HealthCheckService.builder()
            .checkers(
                HealthChecker { mqSequenceFactory.alive() },
                HealthChecker { database.sqlQuery("SELECT 1").findOne() != null }
            )
            .longPolling(0)
            .build()

        // Order matters!
        val combined = AccessLogWriter.combined()
        val serverBuilder = Server.builder()
            .service("/health", healthService)
            .accessLogWriter(
                AccessLogWriter { requestLog ->
                    val path = (requestLog.context() as ServiceRequestContext).path()
                    if (path != "/health") {
                        combined.log(requestLog)
                    }
                },
                true
            )

        for (restService in restServices) {
            serverBuilder.annotatedService("/", restService, exceptionHandler)
        }

        val grpcServiceBuilder = GrpcService.builder()
            .supportedSerializationFormats(GrpcSerializationFormats.values())
            .useBlockingTaskExecutor(true)
        for (grpcService in grpcServices) {
            grpcServiceBuilder.addService(ServerInterceptors.intercept(grpcService, exceptionInterceptor))
        }
        val grpcService = grpcServiceBuilder.build()

        return serverBuilder
            .childChannelOption(ChannelOption.SO_KEEPALIVE, true)
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
        jobWorker.work()
    }

    fun start() {
        try {
            startDatabase()
            val server = startServerInBackground()
            startBackgroundWorkers()
            server.join()
        } catch (e: Exception) {
            LOGGER.fatal(e) { "Failed to start server" }
            exitProcess(1)
        }
    }

    private fun startDatabase() {
        withNewDatabaseSessionScope {
            createUsers.createUsers()
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

    val kodein = DI {
        importOnce(commonModule)
        importOnce(startupModule)
        importOnce(restModule)
        importOnce(mqModule)
        importOnce(grpcModule)
        importOnce(databaseModule)
        importOnce(webModule)
        bind<HttpRequestScopedDecorator>() with singleton { HttpRequestScopedDecorator(di) }
    }
    WebServerStarter(kodein).start()
}
