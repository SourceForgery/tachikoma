package com.sourceforgery.rest.unsubscribe

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.server.annotation.ConsumeType
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import com.sourceforgery.rest.RestService
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.logging.logger
import javax.inject.Inject

internal class UnsubscribeRest
@Inject
private constructor(
        val authentication: Authentication,
        val emailDAO: EmailDAO,
        val emailStatusEventDAO: EmailStatusEventDAO
) : RestService {

    // TODO Merge endpoints by having two @ConsumeType values

    @Post("regex:^/unsubscribe/(?<unsubscribeData>.*)")
    @ConsumeType("multipart/form-data")
    fun unsubscribMultipart(@Param("unsubscribeData") unsubscribeDataString: String, formData: String): HttpResponse {
        try {
            if (formData != UNSUBSCRIBE_FORM_DATA) {
                throw IllegalArgumentException("Not valid One-Click unsubscribe form data $formData")
            }

            // TODO Decode unsubscribe data and unsubscribe the email address

        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to unsubscribe $unsubscribeDataString" })
        }
        return HttpResponse.of(HttpStatus.OK)
    }

    @Post("regex:^/unsubscribe/(?<unsubscribeData>.*)")
    @ConsumeType("application/x-www-form-urlencoded")
    fun unsubscribeUrlencoded(@Param("unsubscribeData") unsubscribeDataString: String, formData: String): HttpResponse {
        try {
            if (formData != UNSUBSCRIBE_FORM_DATA) {
                throw IllegalArgumentException("Not valid One-Click unsubscribe form data $formData")
            }

            // TODO Decode unsubscribe data and unsubscribe the email address

        } catch (e: Exception) {
            LOGGER.warn { "Failed to unsubscribe $unsubscribeDataString with error ${e.message}" }
            LOGGER.debug(e, { "Failed to unsubscribe $unsubscribeDataString" })
        }
        return HttpResponse.of(HttpStatus.OK)
    }

    companion object {
        val UNSUBSCRIBE_FORM_DATA = "List-Unsubscribe=One-Click"
        val LOGGER = logger()
    }
}
