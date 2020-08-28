package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.blockedemail.BlockedEmailService
import com.sourceforgery.tachikoma.blockedemail.BlockedEmailServiceGrpcImpl
import com.sourceforgery.tachikoma.emailstatusevent.EmailStatusEventService
import com.sourceforgery.tachikoma.emailstatusevent.EmailStatusEventServiceGrpcImpl
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.auth.LoginService
import com.sourceforgery.tachikoma.grpc.frontend.auth.LoginServiceGrpcImpl
import com.sourceforgery.tachikoma.identifiers.MessageIdFactory
import com.sourceforgery.tachikoma.identifiers.MessageIdFactoryImpl
import com.sourceforgery.tachikoma.incomingemailaddress.IncomingEmailAddressService
import com.sourceforgery.tachikoma.incomingemailaddress.IncomingEmailAddressServiceGrpcImpl
import com.sourceforgery.tachikoma.maildelivery.impl.IncomingEmailService
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
import com.sourceforgery.tachikoma.users.UserService
import com.sourceforgery.tachikoma.users.UserServiceGrpcImpl
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val grpcModule = DI.Module("grpc") {
    bind<MTADeliveryNotifications>() with singleton { MTADeliveryNotifications(di) }
    bind<MTAEmailQueueService>() with singleton { MTAEmailQueueService(di) }
    bind<MailDeliveryService>() with singleton { MailDeliveryService(di) }
    bind<IncomingEmailService>() with singleton { IncomingEmailService(di) }
    bind<DeliveryNotificationService>() with singleton { DeliveryNotificationService(di) }
    bind<BlockedEmailService>() with singleton { BlockedEmailService(di) }
    bind<EmailStatusEventService>() with singleton { EmailStatusEventService(di) }

    bind<MTADeliveryServiceGrpcImpl>() with singleton { MTADeliveryServiceGrpcImpl(di) }
    bind<MTAEmailQueueServiceGrpcImpl>() with singleton { MTAEmailQueueServiceGrpcImpl(di) }
    bind<DeliveryNotificationServiceGrpcImpl>() with singleton { DeliveryNotificationServiceGrpcImpl(di) }
    bind<MailDeliveryServiceGrpcImpl>() with singleton { MailDeliveryServiceGrpcImpl(di) }
    bind<BlockedEmailServiceGrpcImpl>() with singleton { BlockedEmailServiceGrpcImpl(di) }
    bind<EmailStatusEventServiceGrpcImpl>() with singleton { EmailStatusEventServiceGrpcImpl(di) }
    bind<IncomingEmailAddressServiceGrpcImpl>() with singleton { IncomingEmailAddressServiceGrpcImpl(di) }
    bind<LoginServiceGrpcImpl>() with singleton { LoginServiceGrpcImpl(di) }
    bind<UserServiceGrpcImpl>() with singleton { UserServiceGrpcImpl(di) }

    bind<IncomingEmailAddressService>() with singleton { IncomingEmailAddressService(di) }
    bind<LoginService>() with singleton { LoginService(di) }
    bind<UserService>() with singleton { UserService(di) }
    bind<GrpcExceptionMap>() with singleton { GrpcExceptionMap(di) }

    importOnce(decoderModule)
    bind<MessageIdFactory>() with singleton { MessageIdFactoryImpl(di) }
}

val decoderModule = DI.Module("decoders") {
    bind<TrackingDecoder>() with singleton { TrackingDecoderImpl(di) }
    bind<UnsubscribeDecoder>() with singleton { UnsubscribeDecoderImpl(di) }
}
