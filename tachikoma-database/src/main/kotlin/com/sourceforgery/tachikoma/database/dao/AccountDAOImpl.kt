package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import io.ebean.EbeanServer
import javax.inject.Inject

class AccountDAOImpl
@Inject
private constructor(
    private val ebeanServer: EbeanServer
) : AccountDAO {
    override fun getByMailDomain(mailDomain: MailDomain) =
        ebeanServer
            .find(AccountDBO::class.java)
            .where()
            .eq("mailDomain", mailDomain.mailDomain)
            .findOne()

    override fun save(account: AccountDBO) = ebeanServer.save(account)

    override fun getById(accountId: AccountId) =
        ebeanServer.find(AccountDBO::class.java, accountId.accountId)!!
}
