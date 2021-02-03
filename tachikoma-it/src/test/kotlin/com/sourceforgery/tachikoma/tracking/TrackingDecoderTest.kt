package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import com.sourceforgery.tachikoma.testModule
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrackingDecoderTest : DIAware {
    override val di = DI {
        importOnce(testModule(), allowOverride = true)
    }
    val trackingDecoder: TrackingDecoder by instance()

    @Test
    fun `should create a tracking url`() {

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

    @Test
    fun `should decode a tracking url`() {

        val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5leGFtcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhTP69ssXUL94-eJ1oLBnOOD6eH-Bg"

        val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

        assertEquals(1001, trackingData.emailId.id)
        assertEquals("http://www.example.com/redirectPath", trackingData.redirectUrl)
    }

    @Test
    fun `should throw if trying to decode an invalid url`() {
        val trackingDataString = "qgYsqgYDCNIPsgYjaHR0cDovL3d3dy5le12tcGxlLmNvbS9yZWRpcmVjdFBhdGiyBhT6_Dgl8dQlgiIc4_X0xVkuX3E5Eg"

        assertFailsWith<RuntimeException> {
            trackingDecoder.decodeTrackingData(trackingDataString)
        }
    }
}
