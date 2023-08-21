package com.sourceforgery.tachikoma.graphql.api.account

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.net.URI

class AccountResponse(
    val id: AccountId,
    val mailDomain: MailDomain,
    val baseUrl: URI?,
)
