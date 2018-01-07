package com.sourceforgery.tachikoma.hk2

import org.glassfish.hk2.api.ServiceLocator

interface HK2RequestContext {
    fun <T> runInScope(task: (ServiceLocator) -> T): T
}