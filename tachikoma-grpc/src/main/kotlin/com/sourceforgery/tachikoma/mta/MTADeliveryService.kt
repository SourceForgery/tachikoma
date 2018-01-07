package com.sourceforgery.tachikoma.mta

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class MTADeliveryService
@Inject
private constructor(
        val emailDAO: EmailDAO
) : MTADeliveryNotificationsGrpc.MTADeliveryNotificationsImplBase() {
    override fun setDeliveryStatus(request: DeliveryNotification?, responseObserver: StreamObserver<Empty>?) {

        super.setDeliveryStatus(request, responseObserver)
    }
}