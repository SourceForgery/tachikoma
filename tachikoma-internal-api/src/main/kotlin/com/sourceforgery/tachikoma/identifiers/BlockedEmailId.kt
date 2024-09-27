package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class BlockedEmailId(val blockedEmailId: Long) {
    override fun toString() = "BlockedEmailId: $blockedEmailId"
}
