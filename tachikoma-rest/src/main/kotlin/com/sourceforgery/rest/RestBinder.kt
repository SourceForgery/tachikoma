package com.sourceforgery.rest

import com.sourceforgery.rest.tracking.TrackingRest
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class RestBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(TrackingRest::class.java)
                .to(RestService::class.java)
                .`in`(Singleton::class.java)
    }
}