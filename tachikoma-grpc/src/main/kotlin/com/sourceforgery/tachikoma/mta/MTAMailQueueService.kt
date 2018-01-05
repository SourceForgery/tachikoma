package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTAEmailQueueService
@Inject
private constructor(
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun emailNotified(request: MTAQueuedNotification?, responseObserver: StreamObserver<Empty>?) {
        super.emailNotified(request, responseObserver)
    }

    override fun getEmails(request: Empty?, responseObserver: StreamObserver<EmailMessage>?) {
        super.getEmails(request, responseObserver)
    }

    override fun incomingEmail(request: IncomingEmailMessage?, responseObserver: StreamObserver<Empty>?) {
        super.incomingEmail(request, responseObserver)
    }
}