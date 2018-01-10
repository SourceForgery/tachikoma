package com.sourceforgery.tachikoma.grpc.catcher

import com.sourceforgery.tachikoma.config.DebugConfig
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusRuntimeException
import java.io.PrintWriter
import java.io.StringWriter

abstract class GrpcExceptionCatcher<in T : Throwable>(
        val debugConfig: DebugConfig
) {
    abstract fun status(t: T): Status

    open fun metadata(t: T) = Metadata()

    open fun throwIt(t: T): Nothing {
        throw StatusRuntimeException(status(t), metadata(t))
    }

    protected fun stackToString(e: Throwable): String {
        if (debugConfig.sendDebugData) {
            StringWriter().use {
                e.printStackTrace(PrintWriter(it))
                return it.toString()
            }
        } else {
            return ""
        }
    }
}