package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import javax.inject.Inject

class MTAEmailQueueService
@Inject
private constructor(
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun emailMTAQueued(request: MTAQueuedNotification?, responseObserver: StreamObserver<Empty>?) {
        super.emailMTAQueued(request, responseObserver)
    }

    override fun getEmails(request: Empty?, responseObserver: StreamObserver<EmailMessage>?) {
        super.getEmails(request, responseObserver)
    }
}