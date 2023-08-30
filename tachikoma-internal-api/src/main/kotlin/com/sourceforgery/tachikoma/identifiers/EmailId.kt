package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class EmailId(val emailId: Long) {
    override fun toString() = "EmailId: $emailId"

    companion object {
        fun fromList(emailIds: List<Long>) = emailIds.map { EmailId(it) }
    }
}