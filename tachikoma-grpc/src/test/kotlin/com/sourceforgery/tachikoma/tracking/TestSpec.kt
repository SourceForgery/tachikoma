package com.sourceforgery.tachikoma.tracking

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
object TestSpec: Spek({
    describe("a calculator") {
        it("should add") {
            val sum = 2 + 4
            assertEquals(6, sum)
        }
        it("should subtract") {
            val sum = 2 - 4
            assertEquals(-2, sum)
        }
    }
})