package com.sourceforgery.tachikoma.identifiers

data class AuthenticationId(val authenticationId: Long) {
    override fun toString() = "AuthenticationId: $authenticationId"
}
