package com.sourceforgery.jersey.uribuilder

import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals

class URIUtilTest {
    @Test
    fun `remove authority from url`() {
        assertEquals(URI("https://REDACTED@example.com/query?param=1"), URI("https://foo:bar@example.com/query?param=1").withoutPassword())
    }

    @Test
    fun `add port to incomplete url`() {
        assertEquals(URI("https://example.com:443/query?param=1"), URI("https://example.com/query?param=1").addPort())
        assertEquals(URI("https://foo:bar@example.com:443/query?param=1"), URI("https://foo:bar@example.com/query?param=1").addPort())
        assertEquals(URI("https://foo:bar@example.com:6312/query?param=1"), URI("https://foo:bar@example.com:6312/query?param=1").addPort())
        assertEquals(URI("http://foo:bar@example.com:80/query?param=1"), URI("http://foo:bar@example.com/query?param=1").addPort())
    }
}
