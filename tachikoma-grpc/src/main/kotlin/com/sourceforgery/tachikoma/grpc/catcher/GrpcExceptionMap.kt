package com.sourceforgery.tachikoma.grpc.catcher

import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances

class GrpcExceptionMap(override val di: DI) : DIAware {
    private val catchers by allInstances<GrpcExceptionCatcher<Throwable>>()
    private val map = ConcurrentHashMap<Class<Throwable>, GrpcExceptionCatcher<Throwable>>()

    private val defaultCatcher = object : GrpcExceptionCatcher<Throwable>(Throwable::class.java), DIAware {
        override val di: DI = this@GrpcExceptionMap.di

        override fun logError(t: Throwable) {
            logger.warn(t) { "Exception in gRPC" }
        }

        override fun status(t: Throwable): Status {
            val stackToString = stackToString(t)
            return Status.fromThrowable(t).withDescription(stackToString.substring(0, Math.min(stackToString.length, 6000)))
        }
    }

    fun findCatcher(key: Throwable): GrpcExceptionCatcher<Throwable> {
        @Suppress("UNCHECKED_CAST")
        val clazz = key::class.java as Class<Throwable>
        return map.computeIfAbsent(clazz) { findClass(clazz) }
    }

    private fun getGenerics(catcher: GrpcExceptionCatcher<*>): Type {
        val genericSuperclass = catcher.javaClass.genericSuperclass!!
        return (genericSuperclass as ParameterizedType).actualTypeArguments[0]
    }

    private fun findClass(key: Class<Throwable>): GrpcExceptionCatcher<Throwable> {
        var clazz: Class<*> = key
        while (clazz != Object::class.java) {
            catchers.firstOrNull { getGenerics(it) == clazz }
                ?.let {
                    return it
                }
            clazz = clazz.superclass
        }
        return defaultCatcher
    }

    fun findAndConvertAndLog(t: Throwable): StatusRuntimeException {
        val catcher = findCatcher(t)
        catcher.logError(t)
        return catcher.toException(t)
    }
}
