package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class EmailStatusId(val emailStatusId: Long) {
    override fun toString() = "EmailStatusId: $emailStatusId"
}