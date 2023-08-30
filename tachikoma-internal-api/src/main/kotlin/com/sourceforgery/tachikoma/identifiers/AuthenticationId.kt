package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class AuthenticationId(val authenticationId: Long) {
    // Don't change. Used in template strings
    override fun toString() = authenticationId.toString()
}
