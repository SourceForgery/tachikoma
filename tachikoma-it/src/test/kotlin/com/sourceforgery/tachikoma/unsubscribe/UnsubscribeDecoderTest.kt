package com.sourceforgery.tachikoma.unsubscribe

import com.sourceforgery.tachikoma.grpc.frontend.EmailId
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.testModule
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnsubscribeDecoderTest : DIAware {
    override val di =
        DI {
            importOnce(testModule(), allowOverride = true)
        }
    val unsubscribeDecoder: UnsubscribeDecoder by instance()

    @Test
    fun `should create an unsubscribe url`() {
        val emailId =
            EmailId
                .newBuilder()
                .setId(999)
                .build()

        val unsubscribeData =
            UnsubscribeData
                .newBuilder()
                .setEmailId(emailId)
                .build()

        val url = unsubscribeDecoder.createUrl(unsubscribeData)

        assertEquals("qgYGqgYDCM4PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8", url)
    }

    @Test
    fun `should decode an unsubscribe url`() {
        val unsubscribeDataString = "qgYGqgYDCM4PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8"

        val unsubscribeData = unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)

        assertEquals(999, unsubscribeData.emailId.id)
    }

    @Test
    fun `should throw if trying to decode an invalid url`() {
        val unsubscribeDataString = "qgYGqgYDCM5PsgYUlGa_Cd7_XRqsoeZRMZPkrcANRy8"

        assertFailsWith<RuntimeException> {
            unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)
        }
    }
}
