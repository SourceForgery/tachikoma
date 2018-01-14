package com.sourceforgery.tachikoma.identifiers

data class IncomingEmailId(val incomingEmailId: Long) {
    override fun toString() = incomingEmailId.toString()
}