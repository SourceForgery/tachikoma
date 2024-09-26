package com.sourceforgery.tachikoma.rest.catchers

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.RequestContext

/** Only marker interface for kodein. Don't implement this **/
interface IRestExceptionCatcher

interface RestExceptionCatcher<T : Throwable> : IRestExceptionCatcher {
    fun handleException(
        ctx: RequestContext?,
        req: HttpRequest?,
        cause: T,
    ): HttpResponse
}
