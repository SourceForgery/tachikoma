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
    override fun <ReqT, RespT> interceptCall(call: ServerCall<ReqT, RespT>, headers: Metadata, next: ServerCallHandler<ReqT, RespT>): ServerCall.Listener<ReqT> {
        val nextCall = next.startCall(call, headers)
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(nextCall) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (e: Exception) {
                    LOGGER.warn("Exception in gRPC", e)
                    rethrowAsStatusException(e)
                }
            }

            override fun onMessage(message: ReqT) {
                try {
                    super.onMessage(message)
                } catch (e: Exception) {
                    LOGGER.warn("Exception in gRPC", e)
                    rethrowAsStatusException(e)
                }
            }

            private fun rethrowAsStatusException(e: Exception): Nothing {
                grpcExceptionCatchers[e::class.java as Class<*>]!!.throwIt(e)
            }
        }
    }

    companion object {
        val LOGGER = logger()
    }
}