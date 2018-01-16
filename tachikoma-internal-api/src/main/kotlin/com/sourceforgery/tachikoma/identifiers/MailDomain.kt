package com.sourceforgery.tachikoma.identifiers

data class MailDomain(val mailDomain: String) {
    // Don't change. Used in template string
    override fun toString() = mailDomain

    init {
        assert(mailDomain.isNotEmpty())
    }
}