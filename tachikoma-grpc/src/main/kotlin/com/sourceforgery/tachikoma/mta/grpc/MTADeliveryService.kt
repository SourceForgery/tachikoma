package com.sourceforgery.tachikoma.mta.grpc

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.mta.DeliveredNotification
import com.sourceforgery.tachikoma.mta.DeliveryNotification
import com.sourceforgery.tachikoma.mta.HardBounceNotification
import com.sourceforgery.tachikoma.mta.MTADeliveryNotificationsGrpc
import com.sourceforgery.tachikoma.mta.SoftBounceNotification
import io.grpc.stub.StreamObserver

class MTADeliveryService : MTADeliveryNotificationsGrpc.MTADeliveryNotificationsImplBase() {
    override fun delivered(request: DeliveredNotification?, responseObserver: StreamObserver<Empty>) {
        super.delivered(request, responseObserver)
        responseObserver.onCompleted()
    }

    override fun hardBounce(request: HardBounceNotification, responseObserver: StreamObserver<Empty>) {
        super.hardBounce(request, responseObserver)
        responseObserver.onCompleted()
    }

    override fun setDeliveryStatus(responseObserver: StreamObserver<Empty>): StreamObserver<DeliveryNotification> {
        responseObserver.onCompleted()
        return object : StreamObserver<DeliveryNotification> {
            override fun onCompleted() {
            }

            override fun onError(t: Throwable?) {
                TODO("Log error") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onNext(value: DeliveryNotification?) {
                TODO("Set delivery message to this") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }

    override fun softBounce(request: SoftBounceNotification, responseObserver: StreamObserver<Empty>) {
        super.softBounce(request, responseObserver)
        responseObserver.onCompleted()
    }
}