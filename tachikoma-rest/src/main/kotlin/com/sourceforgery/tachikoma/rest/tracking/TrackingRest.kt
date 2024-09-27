package com.sourceforgery.tachikoma.rest.tracking

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Default
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Header
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Produces
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageClicked
import com.sourceforgery.tachikoma.mq.MessageOpened
import com.sourceforgery.tachikoma.rest.RestService
import com.sourceforgery.tachikoma.rest.httpRedirect
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import java.util.Base64

internal class TrackingRest(
    override val di: DI,
) : RestService, DIAware, TachikomaScope by di.direct.instance() {
    private val trackingDecoder: TrackingDecoder by instance()
    private val emailDAO: EmailDAO by instance()
    private val emailStatusEventDAO: EmailStatusEventDAO by instance()
    private val remoteIP: RemoteIP by instance()
    private val mqSender: MQSender by instance()

    @Get("regex:^/t/(?<trackingData>.*)")
    @Produces("image/gif")
    fun trackOpen(
        @Param("trackingData") trackingDataString: String,
        @Header("User-Agent") @Default("") userAgent: String,
    ) = scopedFuture {
        if (trackingDataString.endsWith("/1")) {
            actuallyTrackOpen(trackingDataString.removeSuffix("/1"), userAgent)
        } else {
            httpRedirect("/t/$trackingDataString/1")
        }
    }

    private fun actuallyTrackOpen(
        trackingDataString: String,
        userAgent: String,
    ): HttpResponse {
        try {
            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            val email = emailDAO.fetchEmailData(trackingData.emailId.toEmailId())!!
            val emailStatusEvent =
                EmailStatusEventDBO(
                    emailStatus = EmailStatus.OPENED,
                    email = email,
                    metaData =
                        StatusEventMetaData(
                            ipAddress = remoteIP.remoteAddress,
                            userAgent = userAgent,
                        ),
                )
            emailStatusEventDAO.save(emailStatusEvent)

            val notificationMessageBuilder =
                DeliveryNotificationMessage.newBuilder()
                    .setCreationTimestamp(emailStatusEvent.dateCreated!!.toTimestamp())
                    .setEmailMessageId(email.id.emailId)
                    .setMessageOpened(
                        MessageOpened.newBuilder()
                            .setIpAddress(remoteIP.remoteAddress),
                    )
            mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to track invalid link $trackingDataString" }
        }
        return HttpResponse.of(HttpStatus.OK, MediaType.GIF, SMALL_TRANSPARENT_GIF)
    }

    @Get("regex:^/c/(?<trackingData>.*)")
    @Produces("text/html")
    fun trackClick(
        @Param("trackingData") trackingDataString: String,
        @Header("User-Agent") @Default("") userAgent: String,
    ) = scopedFuture {
        if (trackingDataString.endsWith("/1")) {
            actuallyTrackClick(trackingDataString.removeSuffix("/1"), userAgent)
        } else {
            httpRedirect("/c/$trackingDataString/1")
        }
    }

    private fun actuallyTrackClick(
        trackingDataString: String,
        userAgent: String,
    ): HttpResponse {
        try {
            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            val email = emailDAO.fetchEmailData(trackingData.emailId.toEmailId())!!
            val emailStatusEvent =
                EmailStatusEventDBO(
                    emailStatus = EmailStatus.CLICKED,
                    email = email,
                    metaData =
                        StatusEventMetaData(
                            ipAddress = remoteIP.remoteAddress,
                            trackingLink = trackingData.redirectUrl,
                            userAgent = userAgent,
                        ),
                )
            emailStatusEventDAO.save(emailStatusEvent)

            val notificationMessageBuilder =
                DeliveryNotificationMessage.newBuilder()
                    .setCreationTimestamp(emailStatusEvent.dateCreated!!.toTimestamp())
                    .setEmailMessageId(email.id.emailId)
                    .setMessageClicked(
                        MessageClicked.newBuilder()
                            .setIpAddress(remoteIP.remoteAddress)
                            .setClickedUrl(trackingData.redirectUrl),
                    )
            mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())

            return httpRedirect(trackingData.redirectUrl)
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to track invalid link $trackingDataString" }
            return HttpResponse.of(
                HttpStatus.NOT_FOUND,
                MediaType.HTML_UTF_8,
                STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK,
            )
        }
    }

    companion object {
        val SMALL_TRANSPARENT_GIF = Base64.getDecoder().decode("R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==")!!
        val STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK = "<html><body>Broken link</body></html>"
        val LOGGER = logger()
    }
}
