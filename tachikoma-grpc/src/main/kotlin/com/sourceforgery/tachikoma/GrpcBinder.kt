package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.blockedemail.BlockedEmailServiceGrpcImpl
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryServiceGrpcImpl
import com.sourceforgery.tachikoma.mta.MTADeliveryNotifications
import com.sourceforgery.tachikoma.mta.MTADeliveryServiceGrpcImpl
import com.sourceforgery.tachikoma.mta.MTAEmailQueueService
import com.sourceforgery.tachikoma.mta.MTAEmailQueueServiceGrpcImpl
import com.sourceforgery.tachikoma.tracking.DeliveryNotificationService
import com.sourceforgery.tachikoma.tracking.DeliveryNotificationServiceGrpcImpl
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import io.grpc.BindableService
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class GrpcBinder : AbstractBinder() {
    override fun configure() {
        bindAsContract(MTADeliveryNotifications::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(MTAEmailQueueService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(MailDeliveryService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(DeliveryNotificationService::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(MTADeliveryServiceGrpcImpl::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(MTAEmailQueueServiceGrpcImpl::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(DeliveryNotificationServiceGrpcImpl::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(MailDeliveryServiceGrpcImpl::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(BlockedEmailServiceGrpcImpl::class.java)
                .to(BindableService::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(GrpcExceptionMap::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(TrackingDecoderImpl::class.java)
                .to(TrackingDecoder::class.java)
                .`in`(Singleton::class.java)

        bindAsContract(UnsubscribeDecoderImpl::class.java)
                .to(UnsubscribeDecoder::class.java)
                .`in`(Singleton::class.java)
    }
}