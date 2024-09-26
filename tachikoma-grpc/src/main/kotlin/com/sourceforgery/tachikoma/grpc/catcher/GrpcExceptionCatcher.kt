package com.sourceforgery.tachikoma.grpc.catcher

import com.sourceforgery.tachikoma.config.DebugConfig
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale

abstract class KodeinAvoidingGrpcExceptionCatcher {
    init {
        require(this is GrpcExceptionCatcher<*>) {
            "Must only subclass ${GrpcExceptionCatcher::class.java.name}"
        }
    }
}

abstract class GrpcExceptionCatcher<in T : Throwable>(
    clazz: Class<T>,
) : DIAware, KodeinAvoidingGrpcExceptionCatcher() {
    private val debugConfig: DebugConfig by instance()

    protected val logger = logger("grpc.exceptions.${clazz.simpleName.lowercase(Locale.US)}")

    abstract fun status(t: T): Status

    open fun logError(t: T) {}

    open fun metadata(t: T) = Metadata()

    open fun toException(t: T): StatusRuntimeException = StatusRuntimeException(status(t), metadata(t))

    fun throwIt(t: T): Nothing {
        logError(t)
        throw toException(t)
    }

    protected fun stackToString(e: Throwable): String {
        if (debugConfig.sendDebugData) {
            StringWriter().use {
                e.printStackTrace(PrintWriter(it))
                return it.toString()
            }
        } else {
            return e.message ?: ""
        }
    }
}
