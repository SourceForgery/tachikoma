package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData

interface TrackingDecoder {
    fun decodeTrackingData(trackingData: String): UrlTrackingData
    fun createUrl(trackingData: UrlTrackingData): String
}