package com.sourceforgery.tachikoma.graphql.api.coercers

import java.net.URI

object URICoercer : AbstractStringCoercer<URI>() {
    override val clazz = URI::class
    override val description: String =
        """
        A type representing a formatted URI
        """.trimIndent()

    override fun fromString(input: String): URI = URI(input)

    override fun toString(input: URI?): String? = input?.toASCIIString()
}
