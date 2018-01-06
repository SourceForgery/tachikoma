package com.sourceforgery.tachikoma.identifiers

data class UserId(val userId: Long) {
    override fun toString() = "UserId: $userId"
}
