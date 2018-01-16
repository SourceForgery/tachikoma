package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.EbeanServer
import javax.inject.Inject

class AccountDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : AccountDAO {
    override fun getById(accountId: AccountId) =
            ebeanServer.find(AccountDBO::class.java, accountId.accountId)!!
}
