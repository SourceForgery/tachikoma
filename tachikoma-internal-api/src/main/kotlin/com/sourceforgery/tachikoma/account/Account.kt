package com.sourceforgery.tachikoma.account

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.net.URI

interface Account {
    val id: AccountId
    val mailDomain: MailDomain
    val baseUrl: URI?
}
