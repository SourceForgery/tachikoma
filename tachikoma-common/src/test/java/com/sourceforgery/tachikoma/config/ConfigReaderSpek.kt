package com.sourceforgery.tachikoma.config

import java.net.URI
import kotlin.test.assertEquals
import org.junit.Test

class ConfigReaderSpek {
    @Test
    fun `Config file test`() {
        val file = createTempFile()
        file.deleteOnExit()
        file.writeText(
            """
                TEST_THING=foo
                SQL_URL=https://test@example.com/foo
            """.trimIndent()
        )
        System.setProperty("tachikomaConfig", file.absolutePath)
        assertEquals("foo", Conf.testThing)
        assertEquals(URI("https://test@example.com/foo"), Conf.sqlUrl)
    }
}

object Conf {
    val testThing by readConfig("TEST_THING", "bar")
    val sqlUrl by readConfig("SQL_URL", URI(""))
}
