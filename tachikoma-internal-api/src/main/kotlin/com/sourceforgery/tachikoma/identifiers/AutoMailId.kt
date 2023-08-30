package com.sourceforgery.tachikoma.identifiers

// Will be of format "<uuid>@${mailDomain}"
@JvmInline
value class AutoMailId(val autoMailId: String) {
    // Don't change as it's used in templates
    override fun toString() = autoMailId

    val localPart
        get() = autoMailId.substringBefore('@')

    val mailDomain
        get() = MailDomain(autoMailId.substringAfter('@'))
}