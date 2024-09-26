package com.sourceforgery.tachikoma.graphql.api.coercers

import com.sourceforgery.tachikoma.graphql.GraphqlCoercer
import graphql.GraphQLContext
import graphql.language.StringValue
import graphql.schema.CoercingParseValueException
import java.util.Locale

abstract class AbstractStringCoercer<I : Any> : GraphqlCoercer<I, String> {
    @Deprecated("Deprecated in Java")
    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): I =
        try {
            fromString(input.toString())
        } catch (e: Exception) {
            throw CoercingParseValueException("Got exception while trying to parse '$input' message: ${e.message}", e)
        }

    abstract fun fromString(input: String): I

    abstract fun toString(input: I?): String?

    @Deprecated("Deprecated in Java")
    override fun parseLiteral(input: Any): I =
        try {
            check(input is StringValue) { "Input $input is not StringValue" }
            fromString(input.value)
        } catch (e: Exception) {
            throw CoercingParseValueException("Got exception while trying to parse '$input' message: ${e.message}", e)
        }

    @Deprecated("Deprecated in Java")
    @Suppress("UNCHECKED_CAST")
    override fun serialize(dataFetcherResult: Any): String? = toString(dataFetcherResult as I)
}
