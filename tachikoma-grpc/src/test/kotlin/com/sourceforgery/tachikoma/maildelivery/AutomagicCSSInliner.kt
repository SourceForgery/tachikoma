package com.sourceforgery.tachikoma.maildelivery

import junit.framework.Assert.assertEquals
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jsoup.Jsoup
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
class AutomagicCSSInliner : Spek({

    describe("Test AutomagicCSSInliner inline css on simple email") {
        it("Simple Email") {
            val input = this.javaClass.getResource("/cssInliner/simple/input.html").readText()
            val expected = Jsoup.parse(this.javaClass.getResource("/cssInliner/simple/expected.html").readText()).html()
            val actual = inlineStyles(input)?.html()
            assertEquals(expected, actual)
        }
    }

    describe("Test AutomagicCSSInliner inline css on complex email") {
        it("Complex email") {
            val input = this.javaClass.getResource("/cssInliner/complex/input.html").readText()
            val expected = Jsoup.parse(this.javaClass.getResource("/cssInliner/complex/expected.html").readText()).html()
            val actual = Jsoup.parse(inlineStyles(input)?.html()).html()
            assertEquals(expected, actual)
        }
    }
})