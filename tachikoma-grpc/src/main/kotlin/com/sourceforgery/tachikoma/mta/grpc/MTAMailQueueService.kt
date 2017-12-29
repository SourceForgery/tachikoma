package com.sourceforgery.tachikoma.mta.grpc

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.mta.EmailMessage
import com.sourceforgery.tachikoma.mta.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.mta.MTAQueuedNotification
import io.grpc.stub.StreamObserver

class MTAEmailQueueService : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun emailMTAQueued(request: MTAQueuedNotification?, responseObserver: StreamObserver<Empty>?) {
        super.emailMTAQueued(request, responseObserver)
    }

    override fun getEmails(request: Empty?, responseObserver: StreamObserver<EmailMessage>?) {
        super.getEmails(request, responseObserver)
    }
}