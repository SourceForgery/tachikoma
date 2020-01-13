package com.sourceforgery.tachikoma

import javax.inject.Singleton
import org.glassfish.hk2.utilities.binding.AbstractBinder

class MinimalBinder(
    private vararg val singletons: Class<*> = emptyArray()
) : AbstractBinder() {
    override fun configure() {
        for (singleton in singletons) {
            bindAsContract(singleton)
                .`in`(Singleton::class.java)
        }
    }
}

class SmallerBinder(private val config: () -> Unit) : AbstractBinder() {
    override fun configure() {
        config()
    }
}