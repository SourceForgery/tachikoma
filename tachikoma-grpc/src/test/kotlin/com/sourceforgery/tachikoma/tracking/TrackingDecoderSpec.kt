package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.net.URI
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@RunWith(JUnitPlatform::class)
internal class TrackingDecoderSpec
@Inject
constructor(
//        val trackingConfig: TrackingConfig
//        val trackingDecoder: TrackingDecoderImpl
) : Spek({

    // TODO Inject this stuff instead..
    val trackingConfig = object : TrackingConfig {
        override val encryptionKey: String
            get() = "A REALLY NICE KEY FOR TESTING"
        override val baseUrl: URI
            get() = URI.create("https://localhost:8070/")
    }
    val trackingDecoder = TrackingDecoderImpl(trackingConfig)

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

            assertEquals("qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5leGFtcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg==", url)
        }

        it("should decode a tracking url") {

            val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5leGFtcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg=="

            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            assertEquals(1001, trackingData.emailId.id)
            assertEquals("http://www.example.com/redirectPath", trackingData.redirectUrl)
        }

        it("should throw if trying to decode an invalid url") {
            val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5le12tcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg=="

            assertFailsWith<RuntimeException> {
                trackingDecoder.decodeTrackingData(trackingDataString)
            }
        }
    }
})