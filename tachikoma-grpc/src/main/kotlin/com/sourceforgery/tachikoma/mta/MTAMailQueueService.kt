package com.sourceforgery.tachikoma.mta

import io.grpc.stub.StreamObserver

class MTAEmailQueueService : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun getEmails(responseObserver: StreamObserver<EmailMessage>?): StreamObserver<MTAQueuedNotification> {
        return super.getEmails(responseObserver)
    }
}