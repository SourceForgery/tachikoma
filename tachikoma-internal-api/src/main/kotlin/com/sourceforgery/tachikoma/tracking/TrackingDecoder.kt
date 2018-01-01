package com.sourceforgery.tachikoma.tracking

import com.sourceforgery.tachikoma.trackingdata.UrlTrackingData

interface TrackingDecoder {
    fun decodeTrackingData(trackingData: String): UrlTrackingData
    fun createUrl(trackingData: UrlTrackingData): String
}