package com.sourceforgery.jersey.uribuilder

import java.net.URI
import kotlin.test.assertEquals
import org.junit.Test

class JerseyUriBuilderTest {
    @Test
    fun `simple test`() {
        val uri = JerseyUriBuilder(URI("https://www.google.com/ping"))
            .queryParam("sitemap", "https://example.com/query?test=bar")
            .build()
        assertEquals(URI("https://www.google.com/ping?sitemap=https%3A%2F%2Fexample.com%2Fquery%3Ftest%3Dbar"), uri)
    }
}