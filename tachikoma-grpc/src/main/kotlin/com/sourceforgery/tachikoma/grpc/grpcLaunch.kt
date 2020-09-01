package com.sourceforgery.tachikoma.grpc

import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.util.concurrent.ExecutionException
import kotlinx.coroutines.future.asCompletableFuture

fun <T> TachikomaScope.grpcFuture(responseObserver: StreamObserver<T>, block: suspend () -> Unit) {
    scopedAsync(
        setup = { _, invokeCounter ->
            (responseObserver as ServerCallStreamObserver<T>).setOnCancelHandler {
                invokeCounter.dump()
            }
        },
        block = block
    )
}

/**
 * Make a Future<StreamObserver> to release Dispatcher as soon as possible.
 * There's no disconnect, so if the dispatcher sends a message immediately,
 * it *will* be blocked until the requestObserver is properly returned
 */
@Suppress("UNCHECKED_CAST")
fun <S, T> TachikomaScope.grpcFutureBidi(responseObserver: StreamObserver<S>, block: suspend () -> StreamObserver<T>): StreamObserver<T> {
    val realRequestObserver = scopedAsync(
        setup = { _, invokeCounter ->
            (responseObserver as ServerCallStreamObserver<T>).setOnCancelHandler {
                invokeCounter.dump()
            }
        },
        block = block
    ).asCompletableFuture()
    return object : StreamObserver<T> {
        private fun observer() = try {
            realRequestObserver.get()
        } catch (e: ExecutionException) {
            throw e.cause!!
        }

        override fun onNext(value: T) {
            observer().onNext(value)
        }

        override fun onError(t: Throwable?) {
            observer().onError(t)
        }

        override fun onCompleted() {
            observer().onCompleted()
        }
    }
}