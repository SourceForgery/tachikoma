package com.sourceforgery.tachikoma

import com.linecorp.armeria.server.ServiceRequestContext
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.hk2.ReqCtxInstance
import javax.inject.Inject
import org.glassfish.hk2.api.ServiceLocator

class TestHK2RequestContext
@Inject
private constructor(
    private val serviceLocator: ServiceLocator
) : HK2RequestContext {
    override fun <T> runInScope(ctx: ReqCtxInstance, task: (ServiceLocator) -> T) = runInNewScope(task)

    override fun getContextInstance(): ReqCtxInstance {
        return object : ReqCtxInstance {}
    }

    override fun <T> runInNewScope(task: (ServiceLocator) -> T): T {
        return task(serviceLocator)
    }

    override fun createInArmeriaContext(serviceRequestContext: ServiceRequestContext) = TODO("not implemented")
    override fun createInstance() = TODO("not implemented")
    override fun release(ctx: ReqCtxInstance) = TODO("not implemented")
}