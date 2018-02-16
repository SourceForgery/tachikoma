package com.sourceforgery.rest.unsubscribe

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.ConsumeType
import com.linecorp.armeria.server.annotation.ConsumeTypes
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import com.sourceforgery.rest.RestService
import com.sourceforgery.rest.tracking.TrackingRest
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageUnsubscribed
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import io.netty.util.AsciiString
import java.text.MessageFormat
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

            createAndSendUnsubscribeEvent(unsubscribeDataString)
        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to unsubscribe $unsubscribeDataString" })
        }
        return HttpResponse.of(HttpStatus.OK)
    }

    @Get("regex:^/clickunsubscribe/(?<unsubscribeData>.*)")
    fun unsubscribe(
            @Param("unsubscribeData") unsubscribeDataString: String
    ): HttpResponse {
        try {
            val unsubscribeData = createAndSendUnsubscribeEvent(unsubscribeDataString)
            val redirectUrl = unsubscribeData.redirectUrl
            if (redirectUrl.isBlank()) {
                return HttpResponse.of(HttpStatus.OK)
            } else {
                return HttpResponse.of(
                        HttpStatus.TEMPORARY_REDIRECT,
                        MediaType.HTML_UTF_8,
                        TrackingRest.HTML_PAGE_WITH_JAVASCRIPT_AND_HTTP_EQUIV_REDIRECT.format(arrayOf(redirectUrl)),
                        HttpHeaders.of(TrackingRest.LOCATION, redirectUrl)
                )
            }
        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to unsubscribe $unsubscribeDataString" })
            return HttpResponse.of(HttpStatus.OK)
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
        val LOCATION = AsciiString.of("Location")!!
        val HTML_PAGE_WITH_JAVASCRIPT_AND_HTTP_EQUIV_REDIRECT = MessageFormat("""
            <html>
              <head>
                <meta http-equiv="refresh" content="0;URL=''{0}''" />
              </head>
              <body>
                <script type="text/javascript">document.location.href=''{0}'';</script>
                <a href="{0}">redirect</a>
              </body>
            </html>
            """.trimMargin())
        val LOGGER = logger()
    }
}
