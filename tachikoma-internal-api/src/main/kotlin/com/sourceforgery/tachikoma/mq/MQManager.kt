package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface MQManager {
    fun setupAccount(mailDomain: MailDomain)

    fun removeAccount(mailDomain: MailDomain)

    fun setupAuthentication(
        mailDomain: MailDomain,
        authenticationId: AuthenticationId,
        accountId: AccountId,
    )

    fun removeAuthentication(authenticationId: AuthenticationId)
}
