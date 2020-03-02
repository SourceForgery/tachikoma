package com.sourceforgery.tachikoma.rest

import com.linecorp.armeria.common.HttpData
import com.linecorp.armeria.common.HttpHeaders
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import io.netty.util.AsciiString
import java.nio.charset.StandardCharsets
import java.text.MessageFormat

object RestUtil {

    private val HTML_PAGE_WITH_REDIRECT_LINK = MessageFormat("""
            <html>
              <body>
                <a href="{0}">redirect</a>
              </body>
            </html>
            """.trimMargin())
    private val LOCATION = AsciiString.of("Location")!!

    fun httpRedirect(redirectUrl: String): HttpResponse {
        return HttpResponse.of(
            HttpStatus.TEMPORARY_REDIRECT,
            MediaType.HTML_UTF_8,
            HttpData.of(
                StandardCharsets.UTF_8,
                HTML_PAGE_WITH_REDIRECT_LINK.format(arrayOf(redirectUrl))
            ),
            HttpHeaders.of(LOCATION, redirectUrl)
        )
    }
}
