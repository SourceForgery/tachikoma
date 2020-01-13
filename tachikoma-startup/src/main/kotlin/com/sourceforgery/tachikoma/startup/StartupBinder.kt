package com.sourceforgery.tachikoma.startup

import com.sourceforgery.tachikoma.config.Configuration
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.config.WebServerConfig
import com.sourceforgery.tachikoma.config.WebtokenAuthConfig
import com.sourceforgery.tachikoma.mq.MqConfig
import com.sourceforgery.tachikoma.tracking.TrackingConfig
import javax.inject.Singleton
import org.glassfish.hk2.utilities.binding.AbstractBinder

class StartupBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(Configuration::class.java)
            .to(DatabaseConfig::class.java)
            .to(TrackingConfig::class.java)
            .to(MqConfig::class.java)
            .to(WebServerConfig::class.java)
            .to(DebugConfig::class.java)
            .to(WebtokenAuthConfig::class.java)
            .`in`(Singleton::class.java)
    }
}