package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
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
internal class TrackingDecoderSpec : Spek({
    val serviceLocator = ServiceLocatorUtilities.bind(RandomStringUtils.randomAlphanumeric(10), TestBinder())
    val trackingDecoder = serviceLocator.getService(TrackingDecoder::class.java)

    describe("TrackingDecoderSpec") {

        it("should create a tracking url") {

            val emailId = EmailId
                .newBuilder()
                .setId(1001)
                .build()

            val trackingData = UrlTrackingData
                .newBuilder()
                .setEmailId(emailId)
                .setRedirectUrl("http://www.example.com/redirectPath")
                .build()

            val url = trackingDecoder.createUrl(trackingData)

            assertEquals("qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5leGFtcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhTP69ssXUL94-eJ1oLBnOOD6eH-Bg", url)
        }

        it("should decode a tracking url") {

            val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5leGFtcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhTP69ssXUL94-eJ1oLBnOOD6eH-Bg"

            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            assertEquals(1001, trackingData.emailId.id)
            assertEquals("http://www.example.com/redirectPath", trackingData.redirectUrl)
        }

        it("should throw if trying to decode an invalid url") {
            val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5le12tcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg"

            assertFailsWith<RuntimeException> {
                trackingDecoder.decodeTrackingData(trackingDataString)
            }
        }
    }
})