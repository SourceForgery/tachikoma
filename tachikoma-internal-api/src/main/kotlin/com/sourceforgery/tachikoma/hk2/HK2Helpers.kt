package com.sourceforgery.tachikoma.hk2

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import org.glassfish.hk2.api.ServiceLocator

inline operator fun <reified T> ServiceLocator.getValue(nothing: Nothing?, property: KProperty<*>): T =
    getService(T::class.java)

inline operator fun <reified T> (() -> ServiceLocator).getValue(obj: Any?, property: KProperty<*>) =
    this().getService(T::class.java)

inline fun <reified T> hk2(crossinline locator: () -> ServiceLocator) = object : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        locator().getService(T::class.java)
}

fun <T> hk2(clazz: Class<T>, serviceLocator: () -> ServiceLocator): () -> T {
    return {
        serviceLocator().getService(clazz)
    }
}