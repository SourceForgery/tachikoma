package com.sourceforgery.tachikoma.identifiers

data class BlockedEmailId(val blockedEmailId: Long) {
    override fun toString() = "BlockedEmailId: $blockedEmailId"
}