package com.sourceforgery.tachikoma

fun <T> T.onlyIf(condition: Boolean, block: T.() -> Unit): T {
    if (condition) {
        block()
    }
    return this
}
