package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class IncomingEmailAddressDAOImpl(override val di: DI) : IncomingEmailAddressDAO, DIAware {
    private val database: Database by instance()

    override fun save(incomingEmailAddressDBO: IncomingEmailAddressDBO) =
        database.save(incomingEmailAddressDBO)

    override fun getByEmail(email: Email) =
        database.find(IncomingEmailAddressDBO::class.java)
            .where()
            .eq("account.mailDomain", email.domain)
            .or()
            .eq("localPart", email.localPart)
            .eq("localPart", "")
            .endOr()
            .orderBy("localPart DESC")
            .setMaxRows(1)
            .findOne()

    override fun getAll(accountDBO: AccountDBO): List<IncomingEmailAddressDBO> =
        database.find(IncomingEmailAddressDBO::class.java)
            .where()
            .eq("account", accountDBO)
            .findList()

    override fun delete(accountDBO: AccountDBO, localPart: String): Int =
        database.find(IncomingEmailAddressDBO::class.java)
            .where()
            .eq("account", accountDBO)
            .eq("localPart", localPart)
            .delete()
}
