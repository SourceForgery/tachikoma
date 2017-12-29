package com.sourceforgery.tachikoma.common

class NamedEmail(
        val address: Email,
        val name: String
) {
    constructor(
            address: String,
            name: String
    ) : this(
            address = Email(address),
            name = name
    )
}