package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpData
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.common.SessionProtocol
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.server.Route
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.ServiceRequestContext
import com.linecorp.armeria.server.graphql.GraphqlService
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.healthcheck.HealthCheckService
import com.linecorp.armeria.server.healthcheck.HealthChecker
import com.linecorp.armeria.server.logging.AccessLogWriter
import com.sourceforgery.tachikoma.commonModule
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.database.hooks.CreateUsers
import com.sourceforgery.tachikoma.databaseModule
import com.sourceforgery.tachikoma.databaseUpgradesModule
import com.sourceforgery.tachikoma.graphql.GraphqlSchemaGenerator
import com.sourceforgery.tachikoma.graphql.graphqlApiModule
import com.sourceforgery.tachikoma.grpcModule
import com.sourceforgery.tachikoma.kodein.withNewDatabaseSessionScope
import com.sourceforgery.tachikoma.memoizeWithExpiration
import com.sourceforgery.tachikoma.mq.JobWorker
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.mq.mqModule
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.rest.restModule
import com.sourceforgery.tachikoma.startup.startupModule
import com.sourceforgery.tachikoma.tracking.REMOTE_IP_ATTRIB
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.webserver.grpc.GrpcExceptionInterceptor
import com.sourceforgery.tachikoma.webserver.grpc.HttpRequestScopedDecorator
import com.sourceforgery.tachikoma.webserver.hk2.webModule
import com.sourceforgery.tachikoma.webserver.rest.RestExceptionHandlerFunction
import graphql.GraphQL
import io.ebean.Database
import io.grpc.BindableService
import io.grpc.ServerInterceptors
import io.netty.channel.ChannelOption
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Log4J2LoggerFactory
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.io.IoBuilder
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.io.File
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

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
    private val remoteIP: RemoteIP by instance()
    private val schemaGenerator: GraphqlSchemaGenerator by instance()

    private fun startServerInBackground(): CompletableFuture<Void> {
        val externalServicesCheck: Boolean by memoizeWithExpiration(5.seconds) {
            mqSequenceFactory.alive() &&
                database.sqlQuery("SELECT 1").findOne() != null
        }
        val healthService = HealthCheckService.builder()
            .checkers(HealthChecker { externalServicesCheck })
            .longPolling(0)
            .build()

        // Order matters!
        val custom = AccessLogWriter.custom(
            """%{remote.ip}j "%r %s" %{requestLength}L bytes ua:%{User-Agent}i"""
        )
        val serverBuilder = Server.builder()
            .service("/health", healthService)
            .accessLogWriter(
                { requestLog ->
                    val path = (requestLog.context() as ServiceRequestContext).path()
                    if (path != "/health") {
                        custom.log(requestLog)
                    }
                },
                true
            )
            .service(Route.builder().exact("/graphql").methods(HttpMethod.GET).build()) { _, _ ->
                HttpResponse.of(
                    HttpStatus.OK,
                    MediaType.HTML_UTF_8,
                    graphqlPlayground,
                )
            }
            .service(
                "/graphql",
                GraphqlService.builder()
                    .configureGraphql({ graphqlBuilder: GraphQL.Builder ->
                        graphqlBuilder.schema(
                            schemaGenerator.generateGraphqlSchema(),
                        )
                    })
                    .useBlockingTaskExecutor(true)
                    .configureGraphql(listOf())
                    .schemaFile(
                        File.createTempFile("dummy", ".graphqls").also { file ->
                            file.deleteOnExit()
                            file.writeText("type Query { thisIsJustDummy: ID }")
                        },
                    )
                    .build()
            )
            .decorator { delegate, ctx, req ->
                try {
                    ctx.setAttr(REMOTE_IP_ATTRIB, remoteIP.remoteAddress)
                } catch (e: Exception) {
                    LOGGER.error(e) { e }
                }
                delegate.serve(ctx, req)
            }

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
                    port(8070, SessionProtocol.HTTPS, SessionProtocol.HTTP, SessionProtocol.PROXY)
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
        private val graphqlPlayground by lazy {
            HttpData.copyOf(
                requireNotNull(WebServerStarter::class.java.getResourceAsStream("/withAnimation.html")) {
                    "Did not find /withAnimation.html"
                }
                    .use { it.readAllBytes() }
            )
        }
        private val LOGGER = logger()
    }
}

@Suppress("unused")
fun main() {
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
        importOnce(databaseUpgradesModule)
        importOnce(graphqlApiModule)
        importOnce(webModule)
        bind<HttpRequestScopedDecorator>() with singleton { HttpRequestScopedDecorator(di) }
    }
    WebServerStarter(kodein).start()
}
