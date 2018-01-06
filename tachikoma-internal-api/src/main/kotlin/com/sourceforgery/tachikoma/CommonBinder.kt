package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.mq.JobFactory
import org.glassfish.hk2.utilities.binding.AbstractBinder
import java.time.Clock
import javax.inject.Singleton

class CommonBinder : AbstractBinder() {
    override fun configure() {
        bind(Clocker())
                .to(Clock::class.java)
        bindAsContract(JobFactory::class.java)
                .`in`(Singleton::class.java)
    }
}
