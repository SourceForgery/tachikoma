package com.sourceforgery.tachikoma.identifiers

data class AuthenticationId(val authenticationId: Long) {
    // Don't change. Used in template strings
    override fun toString() = authenticationId.toString()
}
