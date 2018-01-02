package com.sourceforgery.tachikoma.startup

import com.sourceforgery.rest.RestBinder
import com.sourceforgery.tachikoma.CommonBinder
import com.sourceforgery.tachikoma.GrpcBinder
import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.mq.MqBinder
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class StartupBinder : AbstractBinder() {
    override fun configure() {
        bind(Configuration::class.java)
                .to(DatabaseConfig::class.java)
                .to(TrackingConfig::class.java)
                .`in`(Singleton::class.java)
    }
}

fun bindCommon() =
        ServiceLocatorUtilities.bind(
                CommonBinder(),
                StartupBinder(),
                RestBinder(),
                MqBinder(),
                GrpcBinder()
        )
