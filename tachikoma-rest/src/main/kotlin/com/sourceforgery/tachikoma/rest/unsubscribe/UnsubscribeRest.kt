package com.sourceforgery.tachikoma.rest.unsubscribe

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.Consumes
import com.linecorp.armeria.server.annotation.ConsumesGroup
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageUnsubscribed
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.rest.httpRedirect
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future
import java.time.Clock
import java.util.Optional
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

internal class UnsubscribeRest
@Inject
private constructor(
    private val clock: Clock,
    private val mqSender: MQSender,
    private val unsubscribeDecoder: UnsubscribeDecoder,
    private val emailDAO: EmailDAO,
    private val emailStatusEventDAO: EmailStatusEventDAO,
    private val blockedEmailDAO: BlockedEmailDAO,
    private val remoteIP: RemoteIP,
    tachikomaScope: TachikomaScope
) : RestService, CoroutineScope by tachikomaScope {

    @Post("regex:^/unsubscribe/(?<unsubscribeData>.*)")
    @ConsumesGroup(Consumes("multipart/form-data"), Consumes("application/x-www-form-urlencoded"))
    fun unsubscribe(
        @Param("unsubscribeData") unsubscribeDataString: String,
        @Param("List-Unsubscribe") optionalListUnsubscribe: Optional<String>
    ) = future {
        try {
            val listUnsubscribe = optionalListUnsubscribe.orElse(null)
            if (listUnsubscribe != ONE_CLICK_FORM_DATA) {
                throw IllegalArgumentException("Not valid One-Click unsubscribe form data $listUnsubscribe")
            }
            createAndSendUnsubscribeEvent(unsubscribeDataString)
        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to unsubscribe $unsubscribeDataString" }
        }
        HttpResponse.of(HttpStatus.OK)
    }

    @Get("regex:^/unsubscribe/(?<unsubscribeData>.*)")
    fun unsubscribe(
        @Param("unsubscribeData") unsubscribeDataString: String
    ) = future {
         if (unsubscribeDataString.endsWith("/1")) {
            actuallyUnsubscribe(unsubscribeDataString.removeSuffix("/1"))
        } else {
            httpRedirect("/unsubscribe/$unsubscribeDataString/1")
        }
    }

    private fun actuallyUnsubscribe(unsubscribeDataString: String): HttpResponse {
        return try {
            val unsubscribeData = createAndSendUnsubscribeEvent(unsubscribeDataString)
            val redirectUrl = unsubscribeData.redirectUrl
            if (redirectUrl.isBlank()) {
                HttpResponse.of(HttpStatus.OK)
            } else {
                httpRedirect(redirectUrl)
            }
        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to unsubscribe $unsubscribeDataString" }
            HttpResponse.of(HttpStatus.OK)
        }
    }

    private fun createAndSendUnsubscribeEvent(unsubscribeDataString: String): UnsubscribeData {
        val unsubscribeData = unsubscribeDecoder.decodeUnsubscribeData(unsubscribeDataString)
        val email = emailDAO.fetchEmailData(unsubscribeData.emailId.toEmailId())!!
        LOGGER.info { "Received unsubscribe event from ${email.recipient} for email ${email.id}" }
        val emailStatusEvent = EmailStatusEventDBO(
            emailStatus = EmailStatus.UNSUBSCRIBE,
            email = email,
            metaData = StatusEventMetaData(
                ipAddress = remoteIP.remoteAddress
            )
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
                    .setIpAddress(remoteIP.remoteAddress)
            )
            .build()
        mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessage)
        return unsubscribeData
    }

    companion object {
        val ONE_CLICK_FORM_DATA = "One-Click"
        val LOGGER = logger()
    }
}
