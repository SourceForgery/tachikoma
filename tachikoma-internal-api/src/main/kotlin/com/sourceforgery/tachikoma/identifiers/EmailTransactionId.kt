package com.sourceforgery.tachikoma.identifiers

data class EmailTransactionId(val emailTransactionId: Long) {
    override fun toString() = "EmailTransactionId: $emailTransactionId"
}