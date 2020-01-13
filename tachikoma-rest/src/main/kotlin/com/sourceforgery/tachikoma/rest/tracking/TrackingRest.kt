package com.sourceforgery.tachikoma.rest.tracking

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Produces
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.toTimestamp
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
import com.sourceforgery.tachikoma.rest.RestUtil
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import java.util.Base64
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

internal class TrackingRest
@Inject
private constructor(
    private val trackingDecoder: TrackingDecoder,
    private val emailDAO: EmailDAO,
    private val emailStatusEventDAO: EmailStatusEventDAO,
    private val remoteIP: RemoteIP,
    private val mqSender: MQSender
) : RestService {
    @Get("regex:^/t/(?<trackingData>.*)")
    @Produces("image/gif")
    fun trackOpen(@Param("trackingData") trackingDataString: String): HttpResponse {
        try {
            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            val email = emailDAO.fetchEmailData(trackingData.emailId.toEmailId())!!
            val emailStatusEvent = EmailStatusEventDBO(
                emailStatus = EmailStatus.OPENED,
                email = email,
                metaData = StatusEventMetaData(
                    ipAddress = remoteIP.remoteAddress
                )
            )
            emailStatusEventDAO.save(emailStatusEvent)

            val notificationMessageBuilder = DeliveryNotificationMessage.newBuilder()
                .setCreationTimestamp(emailStatusEvent.dateCreated!!.toTimestamp())
                .setEmailMessageId(email.id.emailId)
                .setMessageOpened(MessageOpened.newBuilder().setIpAddress(remoteIP.remoteAddress))
            mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to track invalid link $trackingDataString" }
        }
        return HttpResponse.of(HttpStatus.OK, MediaType.GIF, SMALL_TRANSPARENT_GIF)
    }

    @Get("regex:^/c/(?<trackingData>.*)")
    @Produces("text/html")
    fun trackClick(@Param("trackingData") trackingDataString: String): HttpResponse {
        try {
            val trackingData = trackingDecoder.decodeTrackingData(trackingDataString)

            val email = emailDAO.fetchEmailData(trackingData.emailId.toEmailId())!!
            val emailStatusEvent = EmailStatusEventDBO(
                emailStatus = EmailStatus.CLICKED,
                email = email,
                metaData = StatusEventMetaData(
                    ipAddress = remoteIP.remoteAddress,
                    trackingLink = trackingData.redirectUrl
                ))
            emailStatusEventDAO.save(emailStatusEvent)

            val notificationMessageBuilder = DeliveryNotificationMessage.newBuilder()
                .setCreationTimestamp(emailStatusEvent.dateCreated!!.toTimestamp())
                .setEmailMessageId(email.id.emailId)
                .setMessageClicked(MessageClicked.newBuilder()
                    .setIpAddress(remoteIP.remoteAddress)
                    .setClickedUrl(trackingData.redirectUrl)
                )
            mqSender.queueDeliveryNotification(email.transaction.authentication.account.id, notificationMessageBuilder.build())

            return RestUtil.httpRedirect(trackingData.redirectUrl)
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e) { "Failed to track invalid link $trackingDataString" }
            return HttpResponse.of(
                HttpStatus.NOT_FOUND,
                MediaType.HTML_UTF_8,
                STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK
            )
        }
    }

    companion object {
        val SMALL_TRANSPARENT_GIF = Base64.getDecoder().decode("R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==")!!
        val STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK = "<html><body>Broken link</body></html>"
        val LOGGER = logger()
    }
}
