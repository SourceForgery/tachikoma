package com.sourceforgery.tachikoma.identifiers

@JvmInline
value class AccountId(val accountId: Long) {
    // Do not change. Used in template strings
    override fun toString() = accountId.toString()
}
