package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class IncomingEmailAddressDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : IncomingEmailAddressDAO {

    override fun save(incomingEmailAddressDBO: IncomingEmailAddressDBO) =
            ebeanServer.save(incomingEmailAddressDBO)

    override fun getByEmail(email: Email) =
            ebeanServer.find(IncomingEmailAddressDBO::class.java)
                    .where()
                    .eq("account.mailDomain", email.domain)
                    .eq("localPart", email.localPart)
                    .findOne()

    override fun getAll(accountDBO: AccountDBO): List<IncomingEmailAddressDBO> =
            ebeanServer.find(IncomingEmailAddressDBO::class.java)
                    .where()
                    .eq("account", accountDBO)
                    .findList()

    override fun delete(accountDBO: AccountDBO, localPart: String): Int =
            ebeanServer.find(IncomingEmailAddressDBO::class.java)
                    .where()
                    .eq("account", accountDBO)
                    .eq("localPart", localPart)
                    .delete()
}