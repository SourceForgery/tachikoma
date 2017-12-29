package com.sourceforgery.tachikoma.mta.grpc

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.DeliveredNotification
import com.sourceforgery.tachikoma.DeliveryNotification
import com.sourceforgery.tachikoma.EmailMessage
import com.sourceforgery.tachikoma.HardBounceNotification
import com.sourceforgery.tachikoma.MTADeliveryNotificationsGrpc
import com.sourceforgery.tachikoma.MTAEmailQueueGrpc
import com.sourceforgery.tachikoma.MTAQueuedNotification
import com.sourceforgery.tachikoma.SoftBounceNotification
import io.grpc.stub.StreamObserver

class MTAEmailQueueService : MTAEmailQueueGrpc.MTAEmailQueueImplBase() {
    override fun emailMTAQueued(request: MTAQueuedNotification?, responseObserver: StreamObserver<Empty>?) {
        super.emailMTAQueued(request, responseObserver)
    }

    override fun getEmails(request: Empty?, responseObserver: StreamObserver<EmailMessage>?) {
        super.getEmails(request, responseObserver)
    }
}