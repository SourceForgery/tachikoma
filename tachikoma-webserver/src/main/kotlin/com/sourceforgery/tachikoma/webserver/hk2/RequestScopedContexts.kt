package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.common.HttpRequest
import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.hk2.SettableReference
import org.glassfish.hk2.api.TypeLiteral

internal val HTTP_REQUEST_TYPE = object : TypeLiteral<SettableReference<HttpRequest>>() {}.type
internal val REQUEST_CONTEXT_TYPE = object : TypeLiteral<SettableReference<RequestContext>>() {}.type