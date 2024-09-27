package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.mq.MQManager

class TestConsumerFactoryImpl : MQManager {
    override fun setupAccount(mailDomain: MailDomain) {
        // At the moment, do nothing for tests
    }

    override fun removeAccount(mailDomain: MailDomain) {
        // At the moment, do nothing for tests
    }

    override fun setupAuthentication(
        mailDomain: MailDomain,
        authenticationId: AuthenticationId,
        accountId: AccountId,
    ) {
        // At the moment, do nothing for tests
    }

    override fun removeAuthentication(authenticationId: AuthenticationId) {
        // At the moment, do nothing for tests
    }
}
