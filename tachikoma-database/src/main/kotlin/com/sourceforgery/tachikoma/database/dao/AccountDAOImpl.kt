package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class AccountDAOImpl(override val di: DI) : AccountDAO, DIAware {
    private val database: Database by instance()

    override fun get(mailDomain: MailDomain) =
        database
            .find(AccountDBO::class.java)
            .where()
            .eq("mailDomain", mailDomain.mailDomain)
            .findOne()

    override fun save(account: AccountDBO) = database.save(account)

    override fun get(accountId: AccountId) =
        requireNotNull(database.find(AccountDBO::class.java, accountId.accountId)) {
            "Could not find account with id $accountId"
        }
}
