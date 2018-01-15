package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.IncomingEmailAddressDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class IncomingEmailAddressDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : IncomingEmailAddressDAO {
    override fun getByEmail(email: Email) =
            ebeanServer.find(IncomingEmailAddressDBO::class.java)
                    .where()
                    .eq("receiverEmail", email.address)
                    .findOne()
}