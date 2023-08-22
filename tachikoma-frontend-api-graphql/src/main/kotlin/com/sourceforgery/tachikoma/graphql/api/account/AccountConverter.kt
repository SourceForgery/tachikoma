package com.sourceforgery.tachikoma.graphql.api.account

import com.sourceforgery.tachikoma.account.Account
import java.net.URI

fun Account.toAccountResponse(defaultBaseUri: URI) =
    AccountResponse(
        id = id,
        mailDomain = mailDomain,
        baseUrl = baseUrl ?: defaultBaseUri,
    )
