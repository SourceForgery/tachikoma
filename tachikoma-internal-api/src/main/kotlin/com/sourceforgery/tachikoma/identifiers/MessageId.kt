package com.sourceforgery.tachikoma.identifiers

// Will be of format "<uuid>@${mailDomain}"
data class MessageId(val messageId: String) {
    // Don't change as it's used in templates
    override fun toString() = messageId

    val localPart: String
        get() = messageId.split('@')[0]
}