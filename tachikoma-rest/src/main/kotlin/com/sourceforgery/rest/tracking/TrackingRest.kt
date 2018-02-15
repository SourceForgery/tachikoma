package com.sourceforgery.rest.tracking

import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.ProduceType
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.logging.logger
import com.sourceforgery.tachikoma.tracking.RemoteIP
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import io.netty.util.AsciiString
import java.text.MessageFormat
import java.util.Base64
import javax.inject.Inject

internal class TrackingRest
@Inject
private constructor(
        private val trackingDecoder: TrackingDecoder,
        private val emailDAO: EmailDAO,
        private val emailStatusEventDAO: EmailStatusEventDAO,
        private val remoteIP: RemoteIP

) : RestService {
    @Get("regex:^/t/(?<trackingData>.*)")
    @ProduceType("image/gif")
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
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to track invalid link $trackingDataString" })
        }
        return HttpResponse.of(HttpStatus.OK, MediaType.GIF, SMALL_TRANSPARENT_GIF)
    }

    @Get("regex:^/c/(?<trackingData>.*)")
    @ProduceType("text/html")
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

            return HttpResponse.of(
                    HttpStatus.TEMPORARY_REDIRECT,
                    MediaType.HTML_UTF_8,
                    HTML_PAGE_WITH_JAVASCRIPT_AND_HTTP_EQUIV_REDIRECT.format(arrayOf(trackingData.redirectUrl)),
                    HttpHeaders.of(LOCATION, trackingData.redirectUrl)
            )
        } catch (e: Exception) {
            LOGGER.warn { "Failed to track invalid link $trackingDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to track invalid link $trackingDataString" })
            return HttpResponse.of(
                    HttpStatus.NOT_FOUND,
                    MediaType.HTML_UTF_8,
                    STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK
            )
        }
    }

    companion object {
        val LOCATION = AsciiString.of("Location")!!
        val SMALL_TRANSPARENT_GIF = Base64.getDecoder().decode("R0lGODlhAQABAIABAP///wAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==")!!
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
        val STATIC_HTML_PAGE_THAT_SAYS_BROKEN_LINK = "<html><body>Broken link</body></html>"

        val LOGGER = logger()
    }
}
