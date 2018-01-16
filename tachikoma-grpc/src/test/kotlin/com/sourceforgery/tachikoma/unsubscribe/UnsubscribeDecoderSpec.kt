package com.sourceforgery.tachikoma.unsubscribe

import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.tracking.TrackingConfig
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
internal class UnsubscribeDecoderSpec
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
    val unsubscribeDecoder = UnsubscribeDecoderImpl(trackingConfig)

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

            assertEquals("qgYGqgYDCM4PsgYU9TuIZgGKyDsPoOALZKWrLzc4zxw=", url)
        }

        it("should decode an unsubscribe url") {

            val unsubscribeDataString = "qgYGqgYDCM4PsgYU9TuIZgGKyDsPoOALZKWrLzc4zxw="

            val unsubscribeData = unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)

            assertEquals(999, unsubscribeData.emailId.id)
        }

        it("should throw if trying to decode an invalid url") {
            val unsubscribeDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5le12tcGxlLmNvbS8yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg=="

            assertFailsWith<RuntimeException> {
                unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)
            }
        }
    }
})