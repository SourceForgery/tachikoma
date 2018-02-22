package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.common.HmacUtil
import com.sourceforgery.tachikoma.common.randomDelay
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlSignedMessage
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject

internal class TrackingDecoderImpl
@Inject
private constructor(
        trackingConfig: TrackingConfig
) : TrackingDecoder {

    private val encryptionKey = trackingConfig.linkSignKey.toByteArray(StandardCharsets.UTF_8)

    override fun decodeTrackingData(trackingData: String): UrlTrackingData {
        val decoded = Base64.getUrlDecoder().decode(trackingData)!!
        val signedMessage = UrlSignedMessage.parseFrom(decoded)
        val sig = signedMessage.signature
        if (!sig.toByteArray().contentEquals(HmacUtil.hmacSha1(signedMessage.message.toByteArray(), encryptionKey))) {
            randomDelay(LongRange(100, 250)) {
                throw RuntimeException("Not correct signature")
            }
        }
        return UrlTrackingData.parseFrom(signedMessage.message)
    }

    override fun createUrl(trackingData: UrlTrackingData): String {
        val parcelled = trackingData.toByteArray()!!
        val signature = HmacUtil.hmacSha1(parcelled, encryptionKey)
        val signedMessage = UrlSignedMessage.newBuilder()
                .setMessage(ByteString.copyFrom(parcelled))
                .setSignature(ByteString.copyFrom(signature))
                .build()
        return Base64.getUrlEncoder().encodeToString(signedMessage.toByteArray())
    }
}
