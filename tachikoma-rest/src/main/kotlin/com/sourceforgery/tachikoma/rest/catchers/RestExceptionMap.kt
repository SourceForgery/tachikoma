package com.sourceforgery.tachikoma.rest.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.config.DebugConfig
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.allInstances
import org.kodein.di.instance
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

class RestExceptionMap(override val di: DI) : DIAware {
    private val debugConfig: DebugConfig by instance()
    private val catchers by allInstances<IRestExceptionCatcher>()
    private val map = ConcurrentHashMap<Class<Throwable>, RestExceptionCatcher<Throwable>>()

    private val defaultCatcher = object : RestExceptionCatcher<Throwable> {
        override fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: Throwable): HttpResponse {
            return if (debugConfig.sendDebugData) {
                HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, MediaType.PLAIN_TEXT_UTF_8, stackToString(cause))
            } else {
                HttpResponse.of(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    private fun stackToString(e: Throwable): String {
        if (debugConfig.sendDebugData) {
            StringWriter().use {
                e.printStackTrace(PrintWriter(it))
                return it.toString()
            }
        } else {
            return ""
        }
    }

    fun findCatcher(key: Class<Throwable>): RestExceptionCatcher<Throwable> {
        try {
            return map.computeIfAbsent(key) { findClass(key) }
        } catch (e: Exception) {
            LOGGER.error(e) { "Failed to map exception" }
            throw e
        }
    }

    private fun getGenerics(catcher: RestExceptionCatcher<*>): Type {
        @Suppress("UNCHECKED_CAST")

        return catcher.javaClass.genericInterfaces
            .filterIsInstance(ParameterizedType::class.java)
            .firstOrNull { it.rawType == RestExceptionCatcher::class.java }!!
            .actualTypeArguments[0]
    }

    private fun findClass(key: Class<Throwable>): RestExceptionCatcher<Throwable> {
        var clazz: Class<*> = key
        while (clazz != Object::class.java) {
            catchers
                .filterIsInstance<RestExceptionCatcher<Throwable>>()
                .firstOrNull { getGenerics(it) == clazz }
                ?.let {
                    return it
                }
            clazz = clazz.superclass
        }
        return defaultCatcher
    }

    companion object {
        private val LOGGER = logger()
    }
}
