package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.mta.MTADeliveryService
import com.sourceforgery.tachikoma.mta.MTAEmailQueueService
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import io.grpc.BindableService
import org.glassfish.hk2.utilities.binding.AbstractBinder

class GrpcBinder : AbstractBinder() {
    override fun configure() {
        bind(MTADeliveryService::class.java)
                .to(BindableService::class.java)
        bind(MTAEmailQueueService::class.java)
                .to(BindableService::class.java)
        bind(TrackingDecoderImpl::class.java)
                .to(TrackingDecoder::class.java)
    }
}