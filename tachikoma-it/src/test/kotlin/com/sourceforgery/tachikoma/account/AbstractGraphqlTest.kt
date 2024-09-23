package com.sourceforgery.tachikoma.account

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.linecorp.armeria.server.Server
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.graphql.graphqlApiModule
import com.sourceforgery.tachikoma.testModule
import com.sourceforgery.tachikoma.webserver.graphql.GraphqlServiceProvider
import com.sourceforgery.tachikoma.webserver.webModule
import io.ebean.Database
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import org.junit.After
import org.junit.Before
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.net.URL
import java.time.Duration
import java.util.concurrent.TimeUnit

abstract class AbstractGraphqlTest : DIAware {
    override val di =
        DI {
            importOnce(testModule(), allowOverride = true)
            importOnce(webModule)
            importOnce(graphqlApiModule)
        }

    val database: Database by instance()
    private val graphqlServiceProvider: GraphqlServiceProvider by instance()

    lateinit var server: Server
    lateinit var client: GraphQLKtorClient

    fun startServer(): Server {
        // Order matters!
        val serverBuilder = Server.builder()
        val server =
            graphqlServiceProvider
                .addGraphqlService(serverBuilder)
                // Grpc must be last
                .requestTimeout(Duration.ofMinutes(1))
                .build()
        server.start().get()

        return server
    }

    fun startClient(): GraphQLKtorClient {
        HttpClient(engineFactory = OkHttp) {
            engine {
                config {
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(600, TimeUnit.SECONDS)
                    writeTimeout(600, TimeUnit.SECONDS)
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
        }
        return GraphQLKtorClient(
            url = URL("http://localhost:${server.activeLocalPort()}/graphql"),
            httpClient = HttpClient(engineFactory = CIO),
            serializer = GraphQLClientJacksonSerializer(),
        )
    }

    fun HttpRequestBuilder.addApitoken(auth: AuthenticationDBO) {
        val apitoken = requireNotNull(auth.apiToken) { "Auth " }
        header("x-apitoken", "${auth.account.mailDomain.mailDomain}:$apitoken")
    }

    @Before
    fun before() {
        server = startServer()
        client = startClient()
    }

    @After
    fun after() {
        client.close()
        server.stop()
    }
}
