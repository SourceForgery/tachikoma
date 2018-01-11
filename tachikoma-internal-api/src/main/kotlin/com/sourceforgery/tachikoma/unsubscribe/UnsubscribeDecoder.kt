package com.sourceforgery.tachikoma.unsubscribe

import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData

interface UnsubscribeDecoder {
    fun decodeUnsubscribeData(unsubscribeData: String): UnsubscribeData
    fun createUrl(unsubscribeData: UnsubscribeData): String
}