package com.sourceforgery.tachikoma.unsubscribe

import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.apache.commons.lang3.RandomStringUtils
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
internal class UnsubscribeDecoderSpec : Spek({
    val serviceLocator = ServiceLocatorUtilities.bind(RandomStringUtils.randomAlphanumeric(10), TestBinder())
    val unsubscribeDecoder = serviceLocator.getService(UnsubscribeDecoder::class.java)

    describe("UnsubscribeDecoderSpec") {

        it("should create an unsubscribe url") {

            val emailId = EmailId
                .newBuilder()
                .setId(999)
                .build()

            val unsubscribeData = UnsubscribeData
                .newBuilder()
                .setEmailId(emailId)
                .build()

            val url = unsubscribeDecoder.createUrl(unsubscribeData)

            assertEquals("qgYGqgYDCM4PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8", url)
        }

        it("should decode an unsubscribe url") {

            val unsubscribeDataString = "qgYGqgYDCM4PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8"

            val unsubscribeData = unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)

            assertEquals(999, unsubscribeData.emailId.id)
        }

        it("should throw if trying to decode an invalid url") {
            val unsubscribeDataString = "qgYGqgYDCM5PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8"

            assertFailsWith<RuntimeException> {
                unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)
            }
        }
    }
})