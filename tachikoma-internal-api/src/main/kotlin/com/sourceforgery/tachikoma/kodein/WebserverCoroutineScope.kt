package com.sourceforgery.tachikoma.kodein

import com.linecorp.armeria.common.RequestContext
import com.sourceforgery.tachikoma.logging.InvokeCounter
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asContextElement

/**
 * Coroutine scope in which kodein will correctly supply the thread local RequestContext.
 * Note that it must be constructed only once a request has arrived to ensure the correct
 * context is available!
 */
private class RequestContextAwareCoroutineScope(
    coroutineContext: CoroutineContext,
    requestContext: RequestContext?,
    invokeCounter: InvokeCounter
) : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        coroutineContext +
            threadLocalLogEverything.asContextElement(invokeCounter)
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
fun CoroutineScope.withRequestContext(requestContext: RequestContext?, invokeCounter: InvokeCounter): CoroutineScope {
    return RequestContextAwareCoroutineScope(this.coroutineContext, requestContext, invokeCounter)
}

/**
 * Augments the coroutine context of the scope to include the thread local RequestContext.
 * Kodein will just work inside this scope.
 */
// operator fun CoroutineScope.plus(requestContext: RequestContext?, invokeCounter: InvokeCounter): CoroutineScope {
//     return RequestContextAwareCoroutineScope(this.coroutineContext, requestContext, invokeCounter)
// }

val threadLocalRequestContext: ThreadLocal<RequestContext> = ThreadLocal()
val threadLocalLogEverything: ThreadLocal<InvokeCounter> = ThreadLocal()

fun <T> withInvokeCounter(invokeCounter: InvokeCounter, block: () -> T): T {
    val old = threadLocalLogEverything.get()
    threadLocalLogEverything.set(invokeCounter)
    try {
        return block()
    } finally {
        threadLocalLogEverything.set(old)
        invokeCounter.dump()
    }
}
