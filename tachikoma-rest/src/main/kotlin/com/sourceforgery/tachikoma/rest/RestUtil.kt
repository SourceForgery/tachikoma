package com.sourceforgery.tachikoma.rest

import com.linecorp.armeria.common.HttpHeaderNames
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.ResponseHeaders

fun httpRedirect(redirectUrl: String): HttpResponse {
    return HttpResponse.of(
        ResponseHeaders.of(HttpStatus.TEMPORARY_REDIRECT, HttpHeaderNames.LOCATION, redirectUrl),
    )
}
