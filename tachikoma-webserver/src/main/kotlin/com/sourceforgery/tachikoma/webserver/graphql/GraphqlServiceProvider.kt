package com.sourceforgery.tachikoma.webserver.graphql

import com.linecorp.armeria.common.HttpData
import com.linecorp.armeria.common.HttpMethod
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.Route
import com.linecorp.armeria.server.ServerBuilder
import com.linecorp.armeria.server.graphql.GraphqlService
import com.sourceforgery.tachikoma.exceptions.NoAuthorizationCredentialsException
import com.sourceforgery.tachikoma.graphql.GraphqlSchemaGenerator
import com.sourceforgery.tachikoma.webserver.WebServerStarter
import graphql.ExceptionWhileDataFetching
import graphql.GraphQL
import graphql.execution.AsyncExecutionStrategy
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class GraphqlServiceProvider(override val di: DI) : DIAware {
    private val schemaGenerator: GraphqlSchemaGenerator by instance()

    private class GraphqlExceptionHandler(override val di: DI) : DIAware, DataFetcherExceptionHandler {

        override fun handleException(handlerParameters: DataFetcherExceptionHandlerParameters): CompletableFuture<DataFetcherExceptionHandlerResult> {

            val exception: Throwable = unwrap(handlerParameters.exception)
            val sourceLocation = handlerParameters.sourceLocation
            val path = handlerParameters.path

            val error = ExceptionWhileDataFetching(path, exception, sourceLocation)
            logException(error, exception)

            return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult().error(error).build())
        }

        protected fun logException(error: ExceptionWhileDataFetching, exception: Throwable) {
            when (exception) {
                is NoAuthorizationCredentialsException -> LOGGER.info("Failed auth for ${exception.message}")
                else -> LOGGER.warn(exception) { exception.message }
            }
        }

        /**
         * Called to unwrap an exception to a more suitable cause if required.
         *
         * @param exception the exception to unwrap
         *
         * @return the suitable exception
         */
        protected fun unwrap(exception: Throwable): Throwable {
            if (exception.cause != null) {
                if (exception is CompletionException) {
                    return exception.cause!!
                }
            }
            return exception
        }
    }

    fun addGraphqlService(serverBuilder: ServerBuilder): ServerBuilder {
        val dataFetcherExceptionHandler = GraphqlExceptionHandler(di)
        return serverBuilder
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
                        graphqlBuilder.schema(schemaGenerator.generateGraphqlSchema())
                            .queryExecutionStrategy(AsyncExecutionStrategy(dataFetcherExceptionHandler))
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
    }

    companion object {
        private val LOGGER = logger()

        private val graphqlPlayground by lazy {
            HttpData.copyOf(
                requireNotNull(WebServerStarter::class.java.getResourceAsStream("/withAnimation.html")) {
                    "Did not find /withAnimation.html"
                }
                    .use { it.readAllBytes() }
            )
        }
    }
}
