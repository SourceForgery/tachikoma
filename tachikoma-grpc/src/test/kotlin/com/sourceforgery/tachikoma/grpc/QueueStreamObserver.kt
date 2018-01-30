package com.sourceforgery.tachikoma.grpc

import io.grpc.stub.StreamObserver
import java.util.concurrent.LinkedBlockingQueue

class QueueStreamObserver<V : Any> : StreamObserver<V> {
    val queue = LinkedBlockingQueue<Wrap<V>>()

    override fun onNext(value: V) {
        queue.add(Wrap(value))
    }

    override fun onError(t: Throwable) {
        queue.add(Wrap(t))
    }

    override fun onCompleted() {
        queue.add(Wrap())
    }
}

class Wrap<V : Any>
private constructor(
        private val value: V?,
        private val exception: Throwable?
) {
    constructor(value: V) : this(value, null)
    constructor(exception: Throwable) : this(null, exception)
    constructor() : this(null, null)

    fun get(): V? {
        exception?.let { throw exception }
        return value
    }
}