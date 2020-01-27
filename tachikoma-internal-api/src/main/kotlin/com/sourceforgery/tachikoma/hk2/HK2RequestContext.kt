package com.sourceforgery.tachikoma.hk2

import com.linecorp.armeria.server.ServiceRequestContext
import org.glassfish.hk2.api.ServiceLocator

interface HK2RequestContext {
    fun <T> runInNewScope(task: (ServiceLocator) -> T): T
    fun getContextInstance(): ReqCtxInstance
    fun <T> runInScope(ctx: ReqCtxInstance, task: (ServiceLocator) -> T): T
    fun createInArmeriaContext(serviceRequestContext: ServiceRequestContext): ReqCtxInstance
    fun createInstance(): ReqCtxInstance
    fun release(ctx: ReqCtxInstance)
}

interface ReqCtxInstance