package com.sourceforgery.tachikoma.common

import java.time.Clock
import java.time.ZoneId

open class Clocker(
    var clock: Clock = systemUTC()
) : Clock() {

    override fun withZone(zone: ZoneId) = Clocker(clock.withZone(zone))

    override fun getZone() = clock.zone

    override fun instant() = clock.instant()
}
