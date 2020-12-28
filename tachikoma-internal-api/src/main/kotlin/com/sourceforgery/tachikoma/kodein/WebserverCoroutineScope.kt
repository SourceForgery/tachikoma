package com.sourceforgery.tachikoma.kodein

import com.linecorp.armeria.common.RequestContext
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

/**
 * Coroutine scope in which kodein will correctly supply the thread local RequestContext.
 * Note that it must be constructed only once a request has arrived to ensure the correct
 * context is available!
 */
private class RequestContextAwareCoroutineScope(
    coroutineContext: CoroutineContext,
    requestContext: RequestContext?,
    databaseSessionContext: DatabaseSessionContext
) : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        coroutineContext +
            threadLocalDatabaseSessionScope.asContextElement(databaseSessionContext)
                .let {
                    if (requestContext != null) {
                        it + threadLocalRequestContext.asContextElement(requestContext)
                    } else {
                        it
                    }
                }
}

/**
 * Augments the coroutine context of the scope to include the thread local RequestContext.
 * Kodein will just work inside this scope.
 */
fun CoroutineScope.withRequestContext(requestContext: RequestContext?, databaseSessionContext: DatabaseSessionContext): CoroutineScope {
    return RequestContextAwareCoroutineScope(this.coroutineContext, requestContext, databaseSessionContext)
}

val threadLocalRequestContext: ThreadLocal<RequestContext> = ThreadLocal()
val threadLocalDatabaseSessionScope: ThreadLocal<DatabaseSessionContext> = ThreadLocal()

suspend fun <T> withNewDatabaseSessionScopeCtx(block: suspend () -> T): T {
    val databaseSessionContext = DatabaseSessionContext()
    try {
        return withContext(threadLocalDatabaseSessionScope.asContextElement(databaseSessionContext)) {
            block()
        }
    } finally {
        DatabaseSessionKodeinScope.getRegistry(databaseSessionContext).close()
    }
}

fun <T> withNewDatabaseSessionScope(block: () -> T): T {
    val old = threadLocalDatabaseSessionScope.get()
    val databaseSessionContext = DatabaseSessionContext()
    threadLocalDatabaseSessionScope.set(databaseSessionContext)
    try {
        return block()
    } finally {
        threadLocalDatabaseSessionScope.set(old)
        DatabaseSessionKodeinScope.getRegistry(databaseSessionContext).close()
    }
}
