package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import org.glassfish.hk2.api.ServiceLocator
import javax.inject.Inject

class TestHK2RequestContext
@Inject
private constructor(
        private val serviceLocator: ServiceLocator
) : HK2RequestContext {
    override fun <T> runInScope(task: (ServiceLocator) -> T): T {
        return task(serviceLocator)
    }
}