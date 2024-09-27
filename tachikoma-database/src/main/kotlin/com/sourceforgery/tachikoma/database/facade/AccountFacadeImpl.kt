package com.sourceforgery.tachikoma.database.facade

import com.sourceforgery.tachikoma.account.Account
import com.sourceforgery.tachikoma.account.AccountFacade
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.identifiers.MailDomain
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.net.URI

class AccountFacadeImpl(override val di: DI) : AccountFacade, DIAware {
    private val accountDAO: AccountDAO by instance()

    override fun modifyAccount(
        mailDomain: MailDomain,
        baseUrl: URI?,
    ): Account {
        val accountDBO = requireNotNull(accountDAO[mailDomain]) { "No account found for $mailDomain" }
        accountDBO.baseUrl = baseUrl
        accountDAO.save(accountDBO)
        return accountDBO
    }

    override fun get(mailDomain: MailDomain): Account? {
        return accountDAO[mailDomain]
    }
}
