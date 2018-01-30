package com.sourceforgery.tachikoma.grpc.frontend

import io.grpc.stub.StreamObserver

class NullStreamObserver<T> : StreamObserver<T> {
    override fun onError(t: Throwable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCompleted() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onNext(value: T?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}