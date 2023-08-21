package com.sourceforgery.tachikoma.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.DIContext
import org.kodein.di.DITrigger
import org.kodein.di.allInstances
import org.kodein.di.direct
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.javaType

class CustomSchemaGeneratorHooks(override val di: DI) : SchemaGeneratorHooks, DIAware {
    private val hooks by lazy {
        direct.allInstances<GraphqlCoercer<*, *>>().associate {
            it.clazz to
                GraphQLScalarType.newScalar()
                    .name(it.clazz.simpleName)
                    .description(it.description)
                    .coercing(it)
                    .build()
        }
    }

    override fun isValidSuperclass(kClass: KClass<*>): Boolean {
        return kClass.simpleName != "DIAware"
    }

    override fun isValidFunction(kClass: KClass<*>, function: KFunction<*>): Boolean {
        // Method without parameters is most likely returning a wrapper
        // function.valueParameters.isNotEmpty() &&
        // Ignore private/internal methods
        return function.visibility == KVisibility.PUBLIC &&
            // Ignore e.g. equals
            function.name !in ignoredMethods &&
            // Nested object functions don't need their own error handling
            function.annotations.none { it is GQLObjectFunction }
    }

    override fun isValidProperty(kClass: KClass<*>, property: KProperty<*>): Boolean {
        val returnType = property.returnType.javaType.typeName.removeSuffix("<?>")
        LOGGER.trace { "$returnType !in $ignoredTypes = ${returnType !in ignoredTypes}" }
        return returnType !in ignoredTypes
    }

    override fun willGenerateGraphQLType(type: KType): GraphQLType? {
        return hooks[type.classifier as? KClass<*>]
    }

    companion object {
        private val ignoredMethods = setOf(
            "equals",
            "copy",
        )

        private val ignoredTypes = listOf(
            DIContext::class.java.name,
            DI::class.java.name,
            DITrigger::class.java.name,
        ).map { it.removeSuffix("<?>") }
            .toSet()

        private val LOGGER = logger()
    }
}

interface GraphqlCoercer<I : Any, O> : Coercing<I, O> {
    val clazz: KClass<I>
    val description: String
}

@Target(AnnotationTarget.FUNCTION)
annotation class GQLObjectFunction
