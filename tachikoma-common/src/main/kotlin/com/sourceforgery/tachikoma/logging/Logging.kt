package com.sourceforgery.tachikoma.logging

import org.apache.logging.log4j.Logger

/**
 * An interface-based "mixin" to easily add a log val to a class, named by the enclosing class. This allows
 * code like this:
 *
 * ```
 * import com.sourceforgery.tachikoma.com.sourceforgery.tachikoma.logging.Logging
 *
 * class MyClass: Logging {
 *   override val log: Logger = logger()
 * }
 *
 * ```
 *
 * A simpler mechanism is to use the class extension directly, like:
 *
 * ```
 * import org.apache.logging.log4j.kotlin.logger
 *
 * class MyClass {
 *   val log = logger()
 * }
 *
 * ```
 */
interface Logging {
    val log: Logger

    fun logger(): Logger = this.javaClass.logger()
}