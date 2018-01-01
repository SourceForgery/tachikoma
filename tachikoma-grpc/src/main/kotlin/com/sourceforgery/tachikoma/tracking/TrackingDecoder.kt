package com.sourceforgery.tachikoma.tracking

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.trackingdata.UrlSignedMessage
import com.sourceforgery.tachikoma.trackingdata.UrlTrackingData
import java.util.Base64
import javax.inject.Inject

class TrackingDecoderImpl
@Inject
private constructor(
        val trackingConfig: TrackingConfig
) : TrackingDecoder {

    override fun decodeTrackingData(trackingData: String): UrlTrackingData {
        val decoded = Base64.getUrlDecoder().decode(trackingData)!!
        val signedMessage = UrlSignedMessage.parseFrom(decoded)!!
        val sig = signedMessage.signature
        // TODO Validate signature
        return UrlTrackingData.parseFrom(sig)!!
    }

    override fun createUrl(trackingData: UrlTrackingData): String {
        val parcelled = trackingData.toByteArray()!!
        // TODO calculate and add signature
        val signature = ByteArray(0)
        val signedMessage = UrlSignedMessage.newBuilder()
                .setMessage(ByteString.copyFrom(parcelled))
                .setSignature(ByteString.copyFrom(signature))
                .build()
        return Base64.getEncoder().encodeToString(signedMessage.toByteArray())!!
    }
}