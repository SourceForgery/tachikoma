package com.sourceforgery.tachikoma.grpc.catcher

import com.sourceforgery.tachikoma.config.DebugConfig
import io.grpc.Status
import org.glassfish.hk2.api.IterableProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class GrpcExceptionMap
@Inject
private constructor(
        private val catchers: IterableProvider<GrpcExceptionCatcher<Throwable>>,
        private val debugConfig: DebugConfig
) : ConcurrentHashMap<Class<Throwable>, GrpcExceptionCatcher<Throwable>>() {

    private val defaultCatcher = object : GrpcExceptionCatcher<Throwable>(debugConfig) {
        override fun status(t: Throwable) = Status.fromThrowable(t).withDescription(stackToString(t))
    }

    override operator fun get(key: Class<Throwable>): GrpcExceptionCatcher<Throwable> {
        return super.computeIfAbsent(key, { findClass(key) })
    }

    private fun findClass(key: Class<Throwable>): GrpcExceptionCatcher<Throwable> {
        var clazz: Class<*> = key
        while (clazz != Object::class.java) {
            catchers.firstOrNull { it == clazz }
                    ?.let {
                        put(key, it)
                        it
                    }
            clazz = clazz.superclass
        }
        return defaultCatcher
    }

}
