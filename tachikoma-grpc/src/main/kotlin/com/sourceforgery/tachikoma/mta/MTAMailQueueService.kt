package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTAEmailQueueService
@Inject
private constructor(
) : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun getEmails(responseObserver: StreamObserver<EmailMessage>?): StreamObserver<MTAQueuedNotification> {
        return super.getEmails(responseObserver)
    }

    override fun incomingEmail(request: IncomingEmailMessage?, responseObserver: StreamObserver<Empty>?) {
        super.incomingEmail(request, responseObserver)
    }
}