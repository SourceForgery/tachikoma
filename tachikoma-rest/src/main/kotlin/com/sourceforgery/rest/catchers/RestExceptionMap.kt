package com.sourceforgery.rest.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.config.DebugConfig
import com.sourceforgery.tachikoma.logging.logger
import org.glassfish.hk2.api.IterableProvider
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class RestExceptionMap
@Inject
private constructor(
        private val catchers: IterableProvider<RestExceptionCatcher<Throwable>>,
        private val debugConfig: DebugConfig
) : ConcurrentHashMap<Class<Throwable>, RestExceptionCatcher<Throwable>>() {

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

    override operator fun get(key: Class<Throwable>): RestExceptionCatcher<Throwable> {
        return super.computeIfAbsent(key, { findClass(key) })
    }

    private fun findClass(key: Class<Throwable>): RestExceptionCatcher<Throwable> {
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

    companion object {
        private val LOGGER = logger()
    }
}
