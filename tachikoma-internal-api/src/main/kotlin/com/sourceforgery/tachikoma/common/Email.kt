package com.sourceforgery.tachikoma.common

import com.sourceforgery.tachikoma.identifiers.MailDomain

class Email(
        val address: String
) {
    val domain = MailDomain(address.substringAfter('@'))
    val localPart = address.substringBefore('@')

    // Don't change this. Used for string templates
    override fun toString() = address
}