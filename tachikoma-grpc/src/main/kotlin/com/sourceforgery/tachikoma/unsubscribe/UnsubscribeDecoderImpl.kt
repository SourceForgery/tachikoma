package com.sourceforgery.tachikoma.unsubscribe

import com.google.common.hash.Hashing.hmacSha1
import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.common.randomDelay
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.SignedUnsubscribeData
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.Base64

class UnsubscribeDecoderImpl(
    override val di: DI,
) : UnsubscribeDecoder, DIAware {
    private val trackingConfig: TrackingConfig by instance()

    @Suppress("UnstableApiUsage")
    private val trackingHmac by lazy {
        hmacSha1(trackingConfig.linkSignKey)
    }

    override fun decodeUnsubscribeData(unsubscribeData: String): UnsubscribeData {
        val decoded = Base64.getUrlDecoder().decode(unsubscribeData)!!
        val signedMessage = SignedUnsubscribeData.parseFrom(decoded)
        val sig = signedMessage.signature
        if (!sig.toByteArray()!!.contentEquals(trackingHmac.hashBytes(signedMessage.message.toByteArray()).asBytes())) {
            randomDelay(LongRange(100, 250)) {
                throw RuntimeException("Not correct signature")
            }
        }
        return UnsubscribeData.parseFrom(signedMessage.message)
    }

    override fun createUrl(unsubscribeData: UnsubscribeData): String {
        val parcelled = unsubscribeData.toByteArray()!!
        val signature = trackingHmac.hashBytes(parcelled).asBytes()
        val signedMessage =
            SignedUnsubscribeData.newBuilder()
                .setMessage(ByteString.copyFrom(parcelled))
                .setSignature(ByteString.copyFrom(signature))
                .build()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signedMessage.toByteArray())
    }
}
