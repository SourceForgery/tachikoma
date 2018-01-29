package com.sourceforgery.tachikoma.grpc

import io.grpc.stub.StreamObserver

class NullStreamObserver<V> : StreamObserver<V> {
    override fun onNext(value: V) {
    }

    override fun onError(t: Throwable?) {
    }

    override fun onCompleted() {
    }
}