package com.sourceforgery.tachikoma.rest.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext

interface RestExceptionCatcher<T : Throwable> {
    fun handleException(ctx: RequestContext?, req: HttpRequest?, cause: T): HttpResponse
}