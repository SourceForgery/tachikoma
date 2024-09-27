package com.sourceforgery.tachikoma.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.expediagroup.graphql.generator.extensions.print
import com.expediagroup.graphql.generator.toSchema
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.instance
import java.io.File

class GraphqlSchemaGenerator(override val di: DI) : DIAware {
    private val queries: List<Query> by allInstances()
    private val mutations: List<Mutation> by allInstances()
    private val subscriptions: List<Subscription> by allInstances()
    private val schemaGeneratorHooks: CustomSchemaGeneratorHooks by instance()

    fun generateGraphqlSchema() =
        toSchema(
            config =
                SchemaGeneratorConfig(
                    supportedPackages = listOf(GraphqlSchemaGenerator::class.java.packageName),
                    introspectionEnabled = true,
                    hooks = schemaGeneratorHooks,
                    dataFetcherFactoryProvider = SimpleKotlinDataFetcherFactoryProvider(),
                ),
            queries =
                queries
                    .map { TopLevelObject(it) },
            mutations =
                mutations
                    .map { TopLevelObject(it) },
            subscriptions =
                subscriptions
                    .map { TopLevelObject(it) },
        )

    fun storeSchema(file: File) {
        val graphqlSchema = generateGraphqlSchema()
        file.writeText(
            graphqlSchema.print(
                includeIntrospectionTypes = false,
                includeScalarTypes = true,
                includeDefaultSchemaDefinition = true,
                includeDirectives = false,
            ),
        )
    }
}

fun main(args: Array<String>) {
    val outputFile = requireNotNull(args.firstOrNull()) { "Need to specify output file" }
    val di =
        DI {
            importOnce(graphqlApiModule)
        }
    val generator: GraphqlSchemaGenerator by di.instance()
    generator.storeSchema(File(outputFile))
}
