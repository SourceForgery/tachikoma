package com.sourceforgery.tachikoma.identifiers

data class AuthenticationId(val userId: Long) {
    override fun toString() = "AuthenticationId: $userId"
}
