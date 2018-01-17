package com.sourceforgery.tachikoma.webserver.grpc

import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.logging.logger
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import javax.inject.Inject

internal class GrpcExceptionInterceptor
@Inject
private constructor(
        private val grpcExceptionCatchers: GrpcExceptionMap
) : ServerInterceptor {

    private fun<T> runCaught(method: () -> T): T {
        try {
            return method()
        } catch (e: Exception) {
            LOGGER.warn("Exception in gRPC", e)
            rethrowAsStatusException(e)
        }
    }

    private fun rethrowAsStatusException(e: Exception): Nothing {
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

    companion object {
        val LOGGER = logger()
    }
}