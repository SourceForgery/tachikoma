package com.sourceforgery.tachikoma.common

class Email(
        val address: String
) {
    val domain = address.substringAfter('@')
    val localPart = address.substringBefore('@')

    // Don't change this. Used for string templates
    override fun toString() = address
}