package com.sourceforgery.rest

import com.sourceforgery.rest.tracking.TrackingRest
import org.glassfish.hk2.utilities.binding.AbstractBinder

class RestBinder : AbstractBinder() {
    override fun configure() {
        bind(TrackingRest::class.java)
                .to(RestService::class.java)
    }
}