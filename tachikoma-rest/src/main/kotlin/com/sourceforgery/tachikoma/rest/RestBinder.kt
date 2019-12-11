package com.sourceforgery.tachikoma.rest

import com.sourceforgery.tachikoma.rest.catchers.RestExceptionMap
import com.sourceforgery.tachikoma.rest.tracking.TrackingRest
import com.sourceforgery.tachikoma.rest.unsubscribe.UnsubscribeRest
import javax.inject.Singleton
import org.glassfish.hk2.utilities.binding.AbstractBinder

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