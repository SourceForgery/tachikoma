package com.sourceforgery.tachikoma.identifiers

data class AccountId(val accountId: Long) {
    // Do not change. Used in template strings
    override fun toString() = accountId.toString()
}