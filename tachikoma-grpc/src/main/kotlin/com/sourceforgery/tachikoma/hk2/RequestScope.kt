package com.sourceforgery.tachikoma.hk2

import com.google.common.base.MoreObjects
import com.google.common.base.Preconditions.checkState
import com.google.common.collect.Sets
import com.sourceforgery.tachikoma.logging.logger
import org.glassfish.hk2.api.ActiveDescriptor
import org.glassfish.hk2.api.Context
import org.glassfish.hk2.api.ServiceHandle
import org.glassfish.hk2.api.ServiceLocator
import java.util.HashMap
import java.util.UUID
import javax.inject.Inject

class HK2RequestContext
@Inject
private constructor(
        private val serviceLocator: ServiceLocator
) : Context<RequestScoped> {

    private val currentScopeInstance = ThreadLocal<Instance>()
    @Volatile private var isActive = true

    override fun getScope(): Class<out Annotation> {
        return RequestScoped::class.java
    }

    override fun <U : Any> findOrCreate(
            activeDescriptor: ActiveDescriptor<U>,
            root: ServiceHandle<*>
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

        val scopeInstance = currentScopeInstance.get()
        checkState(scopeInstance != null, "Not inside a request scope.")

        return scopeInstance!!
    }

    private fun retrieveCurrent(): Instance? {
        checkState(isActive, "Request scope has been already shut down.")
        return currentScopeInstance.get()
    }

    private fun setCurrent(instance: Instance) {
        checkState(isActive, "Request scope has been already shut down.")
        currentScopeInstance.set(instance)
    }

    private fun resumeCurrent(instance: Instance?) {
        currentScopeInstance.set(instance)
    }

    private fun createInstance(): Instance {
        return Instance()
    }

    fun <T> runInScope(task: (ServiceLocator) -> T): T {
        val oldInstance = retrieveCurrent()
        val instance = createInstance()
        try {
            setCurrent(instance)
            return task(serviceLocator)
        } finally {
            instance.release()
            resumeCurrent(oldInstance)
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal class Instance internal constructor() {
        private val id by lazy {
            UUID.randomUUID()
        }

        private val store = HashMap<ActiveDescriptor<*>, Any>()

        internal operator fun <T> get(descriptor: ActiveDescriptor<T>): T {
            return store[descriptor] as T
        }

        internal fun <T : Any> put(descriptor: ActiveDescriptor<T>, value: T): T {
            checkState(!store.containsKey(descriptor),
                    "An instance for the descriptor %s was already seeded in this scope. Old instance: %s New instance: %s",
                    descriptor,
                    store[descriptor],
                    value)

            return store.put(descriptor, value) as T
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
                LOGGER.debug { "Released scope instance " + this }
            }
        }

        override fun toString(): String {
            return MoreObjects
                    .toStringHelper(this)
                    .add("id", id)
                    .add("store size", store.size).toString()
        }
    }

    companion object {
        val LOGGER = logger()
    }
}