package com.sourceforgery.tachikoma.identifiers

// Will be of format "<uuid>@${mailDomain}"
@JvmInline
value class MessageId(val messageId: String) {
    // Don't change as it's used in templates
    override fun toString() = messageId

    val localPart
        get() = messageId.substringBefore('@')

    val mailDomain
        get() = MailDomain(messageId.substringAfter('@'))
}
