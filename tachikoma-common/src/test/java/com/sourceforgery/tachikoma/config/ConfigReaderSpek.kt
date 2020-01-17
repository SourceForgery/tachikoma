package com.sourceforgery.tachikoma.config

import java.net.URI
import kotlin.test.assertEquals
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class ConfigReaderSpek : Spek({
    describe("Config file test") {
        it("property") {
            val file = createTempFile()
            file.deleteOnExit()
            file.writeText("""
                TEST_THING=foo
                SQL_URL=https://test@example.com/foo
            """.trimIndent())
            System.setProperty("tachikomaConfig", file.absolutePath)
            assertEquals("foo", Conf.testThing)
            assertEquals(URI("https://test@example.com/foo"), Conf.sqlUrl)
        }
    }
})

object Conf {
    val testThing by readConfig("TEST_THING", "bar")
    val sqlUrl by readConfig("SQL_URL", URI(""))
}