package com.sourceforgery.rest

import com.sourceforgery.rest.catchers.RestExceptionMap
import com.sourceforgery.rest.tracking.TrackingRest
import com.sourceforgery.rest.unsubscribe.UnsubscribeRest
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class RestBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(TrackingRest::class.java)
                .to(RestService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(UnsubscribeRest::class.java)
                .to(RestService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(RestExceptionMap::class.java)
                .`in`(Singleton::class.java
                )
    }
}