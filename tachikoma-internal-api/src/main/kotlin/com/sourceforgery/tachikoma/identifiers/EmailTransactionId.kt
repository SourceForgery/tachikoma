package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class EmailTransactionId(val emailTransactionId: Long) {
    override fun toString() = "EmailTransactionId: $emailTransactionId"
}