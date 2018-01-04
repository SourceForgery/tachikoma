package com.sourceforgery.tachikoma.startup

import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class StartupBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(Configuration::class.java)
                .to(DatabaseConfig::class.java)
                .to(TrackingConfig::class.java)
                .to(MqConfig::class.java)
                .to(WebServerConfig::class.java)
                .`in`(Singleton::class.java)
    }
}