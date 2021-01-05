package com.sourceforgery.tachikoma.kodein

import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.bindings.Scope
import org.kodein.di.bindings.ScopeRegistry
import org.kodein.di.bindings.StandardScopeRegistry

object DatabaseSessionKodeinScope : Scope<DatabaseSessionContext> {
    private val map = linkedMapOf<DatabaseSessionContext, StandardScopeRegistry>()

    override fun getRegistry(context: DatabaseSessionContext): ScopeRegistry {
        synchronized(map) {
            return map.getOrPut(context) {
                LOGGER.trace(Throwable("Creating scope from context: $context")) { "Creating scope from context: $context" }
                StandardScopeRegistry()
            }.also {
                LOGGER.trace(Throwable("Returning via $context")) { "Returning via $context" }
            }
        }
    }

    private val LOGGER = logger()
}

class DatabaseSessionContext
