package com.sourceforgery.tachikoma.hk2

import org.glassfish.hk2.api.Factory
import java.util.concurrent.atomic.AtomicReference

class ReferencingFactory<T> : Factory<AtomicReference<T>> {
    override fun dispose(instance: AtomicReference<T>?) {
    }

    override fun provide() = AtomicReference<T>()
}