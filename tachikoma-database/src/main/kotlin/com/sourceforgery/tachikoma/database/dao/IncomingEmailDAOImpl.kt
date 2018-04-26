package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import io.ebean.EbeanServer
import javax.inject.Inject

class IncomingEmailDAOImpl
@Inject
private constructor(
    val ebeanServer: EbeanServer
) : IncomingEmailDAO {
    override fun save(incomingEmailDBO: IncomingEmailDBO) {
        ebeanServer.save(incomingEmailDBO)
    }

    override fun fetchIncomingEmail(incomingEmailId: IncomingEmailId) =
        ebeanServer.find(IncomingEmailDBO::class.java, incomingEmailId.incomingEmailId)
}