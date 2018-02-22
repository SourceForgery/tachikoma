package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.ebean.EbeanServer
import java.util.UUID
import javax.inject.Inject

class DAOHelper
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) {

    fun createAuthentication(domain: MailDomain): AuthenticationDBO {
        val accountDBO = AccountDBO(domain)
        ebeanServer.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
                login = domain.mailDomain,
                encryptedPassword = UUID.randomUUID().toString(),
                apiToken = UUID.randomUUID().toString(),
                role = AuthenticationRole.BACKEND,
                account = accountDBO
        )
        ebeanServer.save(authenticationDBO)

        return authenticationDBO
    }
}