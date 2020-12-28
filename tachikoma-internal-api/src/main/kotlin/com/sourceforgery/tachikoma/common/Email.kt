package com.sourceforgery.tachikoma.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.sourceforgery.tachikoma.identifiers.MailDomain

class Email {
    constructor(domain: MailDomain, localPart: String) {
        this.domain = domain
        this.localPart = localPart
        this.address = "$localPart@$domain"
    }

    @JsonCreator
    constructor(address: String) {
        this.address = address
        domain = MailDomain(address.substringAfter('@'))
        localPart = address.substringBefore('@')
    }

    @get:JsonValue
    val address: String
    val domain: MailDomain
    val localPart: String

    // Don't change this. Used for string templates
    override fun toString() = address

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Email) return false

        return address == other.address
    }

    fun validateEmail() {
        EMAIL_VALIDATOR.matchEntire(address)
    }

    override fun hashCode() = address.hashCode()

    companion object {
        val EMAIL_VALIDATOR = Regex("([%a-zåäö0-9!#$&'*+-/=?^_`{|}~]+)@([-a-zåäö0-9.]+\\.[a-zåäö]{2,})", RegexOption.IGNORE_CASE)
    }
}
