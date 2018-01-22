package com.sourceforgery.tachikoma.identifiers

data class IncomingEmailId(val incomingEmailId: Long) {
    // Keep this toString as it's used in template strings
    override fun toString() = incomingEmailId.toString()
}