package com.sourceforgery.tachikoma.common

import java.time.Clock
import java.time.Clock.systemUTC
import java.time.ZoneId

open class Clocker(clock: Clock = systemUTC()) : Clock() {
    var clock: Clock = clock
        protected set

    override fun withZone(zone: ZoneId) = Clocker(clock.withZone(zone))

    override fun getZone() = clock.zone

    override fun instant() = clock.instant()
}
