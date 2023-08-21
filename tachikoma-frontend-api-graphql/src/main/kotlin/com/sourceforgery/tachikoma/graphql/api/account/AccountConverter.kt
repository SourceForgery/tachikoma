package com.sourceforgery.tachikoma.graphql.api.account

import com.sourceforgery.tachikoma.account.Account

fun Account.toAccountResponse() =
    AccountResponse(
        id = id,
        mailDomain = mailDomain,
        baseUrl = baseUrl,
    )
