package com.sourceforgery.tachikoma

import com.google.common.base.Suppliers
import java.util.concurrent.TimeUnit
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

fun <T> T.onlyIf(condition: Boolean, block: T.() -> Unit): T {
    if (condition) {
        block()
    }
    return this
}

fun <T> memoizeWithExpiration(duration: Duration, method: () -> T): ReadOnlyProperty<Any?, T> =
    object : ReadOnlyProperty<Any?, T> {
        val memoized = Suppliers.memoizeWithExpiration(method, duration.inWholeMilliseconds, TimeUnit.MILLISECONDS)::get

        override fun getValue(thisRef: Any?, property: KProperty<*>): T = memoized()
    }
