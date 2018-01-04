package com.sourceforgery.tachikoma.webserver.hk2

import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.SettableReference
import org.glassfish.hk2.api.Factory
import javax.inject.Inject

class ServiceRequestContextFactory
@Inject
private constructor(
        private val serviceRequestContext: SettableReference<ServiceRequestContext>
) : Factory<ServiceRequestContext> {

    override fun provide(): ServiceRequestContext = serviceRequestContext.value!!

    override fun dispose(instance: ServiceRequestContext?) {
    }
}