package com.sourceforgery.tachikoma.hk2

import org.glassfish.hk2.api.Factory

class ReferencingFactory<T> : Factory<SettableReference<T>> {
    override fun dispose(instance: SettableReference<T>?) {
    }

    override fun provide(): SettableReference<T> = SettableReferenceImpl<T>()
}

interface SettableReference<T> {
    var value: T?
}

internal class SettableReferenceImpl<T> : SettableReference<T> {
    override var value: T? = null
}