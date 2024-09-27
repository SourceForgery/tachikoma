package com.sourceforgery.tachikoma.graphql.api.coercers

import com.sourceforgery.tachikoma.graphql.GraphqlCoercer
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.IntValue
import graphql.language.Value
import graphql.schema.CoercingParseValueException
import java.math.BigInteger
import java.util.Locale

abstract class AbstractIntCoercer<I : Any> : GraphqlCoercer<I, BigInteger> {
    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): I =
        try {
            fromInt(input as BigInteger)
        } catch (e: Exception) {
            throw CoercingParseValueException("Got exception while trying to parse '$input' message: ${e.message}", e)
        }

    abstract fun fromInt(input: BigInteger): I

    abstract fun toInt(input: I?): BigInteger?

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): I =
        try {
            check(input is IntValue) { "Input $input is not IntValue" }
            fromInt(input.value)
        } catch (e: Exception) {
            throw CoercingParseValueException("Got exception while trying to parse '$input' message: ${e.message}", e)
        }

    @Suppress("UNCHECKED_CAST")
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): BigInteger? = toInt(dataFetcherResult as I)
}
