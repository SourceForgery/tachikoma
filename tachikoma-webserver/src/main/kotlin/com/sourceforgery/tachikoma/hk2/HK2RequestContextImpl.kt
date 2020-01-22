package com.sourceforgery.tachikoma.hk2

import com.google.common.base.Preconditions.checkState
import com.google.common.collect.Sets
import com.linecorp.armeria.server.ServiceRequestContext
import io.netty.util.AttributeKey
import java.util.HashMap
import java.util.UUID
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger
import org.glassfish.hk2.api.ActiveDescriptor
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.ServiceHandle
import org.glassfish.hk2.api.ServiceLocator

class HK2RequestContextImpl
@Inject
private constructor(
    private val serviceLocator: ServiceLocator
) : Context<RequestScoped>, HK2RequestContext {

    private val threadLocalScopeInstance = ThreadLocal<Instance>()
    @Volatile
    private var isActive = true

    override fun getScope(): Class<out Annotation> {
        return RequestScoped::class.java
    }

    override fun <U : Any> findOrCreate(
        activeDescriptor: ActiveDescriptor<U>,
        root: ServiceHandle<*>?
    ): U? {

        val instance = current()

        var retVal: U? = instance[activeDescriptor]
        if (retVal == null) {
            retVal = activeDescriptor.create(root)
            instance.put(activeDescriptor, retVal)
        }
        return retVal
    }

    override fun containsKey(descriptor: ActiveDescriptor<*>): Boolean {
        return current().contains(descriptor)
    }

    override fun supportsNullCreation(): Boolean {
        return true
    }

    override fun isActive(): Boolean {
        return isActive
    }

    override fun destroyOne(descriptor: ActiveDescriptor<*>) {
        current().remove(descriptor)
    }

    override fun shutdown() {
        isActive = false
    }

    private fun current(): Instance {
        checkState(isActive, "Request scope has been already shut down.")
        val armeriaCtx = ServiceRequestContext.currentOrNull()
        val instance = if (armeriaCtx == null) {
            threadLocalScopeInstance.get()
                    ?.also {
                        LOGGER.trace { "Getting scope $it from local thread ${Thread.currentThread()}" }
                    }
        } else {
            armeriaCtx.attr(HK2_CONTEXT_KEY).get()
                    ?.also {
                        LOGGER.trace { "Getting scope $it from armeria context $armeriaCtx in thread ${Thread.currentThread()}" }
                    }
        }
        return instance
                ?: error("Not inside a request scope.")
    }

    override fun createInstance(): ReqCtxInstance = Instance()

    override fun createInArmeriaContext(serviceRequestContext: ServiceRequestContext): ReqCtxInstance {
        val instance = createInstance() as Instance
        LOGGER.trace { "Setting scope $instance to armeria context $serviceRequestContext in thread ${Thread.currentThread()}" }
        serviceRequestContext.attr(HK2_CONTEXT_KEY).set(instance)
        return instance
    }

    override fun release(ctx: ReqCtxInstance) {
        (ctx as Instance).release()
    }

    override fun getContextInstance(): ReqCtxInstance = current()

    override fun <T> runInScope(ctx: ReqCtxInstance, task: (ServiceLocator) -> T): T {
        ctx as? Instance
                ?: error("Must be instance from $javaClass")
        try {
            checkState(isActive, "Request scope has been already shut down.")
            threadLocalScopeInstance.set(ctx)
            LOGGER.trace { "Entering request scope" }
            return task(serviceLocator)
        } finally {
            LOGGER.trace { "Leaving request scope" }
            threadLocalScopeInstance.remove()
        }
    }

    override fun <T> runInNewScope(task: (ServiceLocator) -> T): T {
        if (threadLocalScopeInstance.get() != null) {
            error("Already in request scope ${threadLocalScopeInstance.get()}")
        }
        val instance = createInstance() as Instance
        try {
            checkState(isActive, "Request scope has been already shut down.")
            threadLocalScopeInstance.set(instance)
            LOGGER.trace { "Entering request scope" }
            return task(serviceLocator)
        } finally {
            LOGGER.trace { "Leaving request scope" }
            release(instance)
            threadLocalScopeInstance.remove()
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal class Instance internal constructor() : ReqCtxInstance {
        private val id by lazy {
            UUID.randomUUID()
        }

        private val store = HashMap<ActiveDescriptor<*>, Any>()

        internal operator fun <T> get(descriptor: ActiveDescriptor<T>): T {
            return store[descriptor] as T
        }

        internal fun <T : Any> put(descriptor: ActiveDescriptor<T>, value: T): T? {
            checkState(
                !store.containsKey(descriptor),
                "An instance for the descriptor %s was already seeded in this scope. Old instance: %s New instance: %s",
                descriptor,
                store[descriptor],
                value
            )

            return store.put(descriptor, value) as T?
        }

        internal fun <T> remove(descriptor: ActiveDescriptor<T>) {
            store.remove(descriptor)
                ?.let { descriptor.dispose(it as T) }
        }

        fun <T> contains(provider: ActiveDescriptor<T>): Boolean {
            return store.containsKey(provider)
        }

        fun release() {
            try {
                for (descriptor in Sets.newHashSet(store.keys)) {
                    remove<Any>(descriptor as ActiveDescriptor<Any>)
                }
            } finally {
                LOGGER.debug { "Released scope instance $this" }
            }
        }

        override fun toString(): String {
            return "id = $id, store size = ${store.size}"
        }
    }

    companion object {
        val LOGGER = logger()
        private val HK2_CONTEXT_KEY = AttributeKey.valueOf<Instance>("HK2_CONTEXT")
    }
}