package com.sourceforgery.tachikoma.identifiers

data class AccountId(val accountId: Long) {
    override fun toString() = accountId.toString()
}