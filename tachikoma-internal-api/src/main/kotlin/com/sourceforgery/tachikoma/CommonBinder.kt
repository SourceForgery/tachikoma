package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.Clocker
import org.glassfish.hk2.utilities.binding.AbstractBinder
import java.time.Clock

class CommonBinder : AbstractBinder() {
    override fun configure() {
        bind(Clocker())
                .to(Clock::class.java)
    }
}
