package com.sourceforgery.tachikoma.grpc

import io.grpc.stub.StreamObserver
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

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

    fun take(milliseconds: Long): V {
        val wrap = queue.poll(milliseconds, TimeUnit.MILLISECONDS)
                ?: throw AssertionError("Did not get any result in $milliseconds milliseconds")
        return wrap.get()
                ?: throw AssertionError("Didn't expect end of data")
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