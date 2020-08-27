package com.sourceforgery.tachikoma.webserver.grpc

import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

internal class GrpcExceptionInterceptor(
    override val di: DI
) : ServerInterceptor, DIAware {

    private val grpcExceptionCatchers: GrpcExceptionMap by instance()

    private fun <T> runCaught(method: () -> T): T {
        try {
            return method()
        } catch (e: Exception) {
            rethrowAsStatusException(e)
        }
    }

    private fun rethrowAsStatusException(e: Throwable): Nothing {
        grpcExceptionCatchers.findCatcher(e)
            .throwIt(e)
    }

    override fun <ReqT, RespT> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        return runCaught {
            val nextCall = next.startCall(call, headers)
            object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(nextCall) {
                override fun onHalfClose() {
                    runCaught { super.onHalfClose() }
                }

                override fun onMessage(message: ReqT) {
                    runCaught { super.onMessage(message) }
                }
            }
        }
    }
}
