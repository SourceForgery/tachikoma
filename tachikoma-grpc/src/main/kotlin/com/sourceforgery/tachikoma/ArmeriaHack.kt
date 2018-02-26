package com.sourceforgery.tachikoma

import com.linecorp.armeria.common.HttpResponseWriter
import io.grpc.ServerCall
import io.grpc.stub.StreamObserver

fun assertGrpcOpen(responseObserver: StreamObserver<*>) {
    val ServerCallStreamObserverImpl = Class.forName("io.grpc.stub.ServerCalls\$ServerCallStreamObserverImpl")
    val armeriaServerCall = ServerCallStreamObserverImpl
            .getDeclaredField("call")
            .apply {
                isAccessible = true
            }
            .get(responseObserver) as ServerCall<*, *>

    val ArmeriaServerCall = Class.forName("com.linecorp.armeria.server.grpc.ArmeriaServerCall")
    val res = ArmeriaServerCall
            .getDeclaredField("res")
            .apply {
                isAccessible = true
            }
            .get(armeriaServerCall) as HttpResponseWriter
    if (!res.isOpen) {
        throw IllegalArgumentException("Socket already closed")
    }
}