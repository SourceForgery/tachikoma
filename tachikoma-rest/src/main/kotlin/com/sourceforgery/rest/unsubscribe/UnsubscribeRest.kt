package com.sourceforgery.rest.unsubscribe

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.ConsumeType
import com.linecorp.armeria.server.annotation.ConsumeTypes
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageUnsubscribed
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import java.time.Clock
import javax.inject.Inject

internal class UnsubscribeRest
@Inject
private constructor(
        private val clock: Clock,
        private val mqSender: MQSender,
        private val unsubscribeDecoder: UnsubscribeDecoder,
        private val emailDAO: EmailDAO,
        private val emailStatusEventDAO: EmailStatusEventDAO,
        private val blockedEmailDAO: BlockedEmailDAO,
        private val remoteIP: RemoteIP
) : RestService {

    @Post("regex:^/unsubscribe/(?<unsubscribeData>.*)")
    @ConsumeTypes(ConsumeType("multipart/form-data"), ConsumeType("application/x-www-form-urlencoded"))
    fun unsubscribe(
            @Param("unsubscribeData") unsubscribeDataString: String,
            @Param("List-Unsubscribe") listUnsubscribe: String
    ): HttpResponse {
        try {
            if (listUnsubscribe != ONE_CLICK_FORM_DATA) {
                throw IllegalArgumentException("Not valid One-Click unsubscribe form data $listUnsubscribe")
            }

            val unsubscribeData = unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)

            val email = emailDAO.fetchEmailData(unsubscribeData.emailId.toEmailId())!!
            val emailStatusEvent = EmailStatusEventDBO(
                    emailStatus = EmailStatus.UNSUBSCRIBE,
                    email = email
            )
            emailStatusEventDAO.save(emailStatusEvent)
            blockedEmailDAO.block(emailStatusEvent)

            val notificationMessage = DeliveryNotificationMessage
                    .newBuilder()
                    .setCreationTimestamp(clock.instant().toTimestamp())
                    .setEmailMessageId(email.id.emailId)
                    .setMessageUnsubscribed(
                            MessageUnsubscribed
                                    .newBuilder()
                                    .setIpAdress(remoteIP.remoteAddress)
                    )
                    .build()
            mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessage)
        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to unsubscribe $unsubscribeDataString" })
        }
        return HttpResponse.of(HttpStatus.OK)
    }

    companion object {
        val ONE_CLICK_FORM_DATA = "One-Click"
        val LOGGER = logger()
    }
}
