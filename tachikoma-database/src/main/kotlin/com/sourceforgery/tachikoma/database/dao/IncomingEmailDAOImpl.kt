package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class IncomingEmailDAOImpl(override val di: DI) : IncomingEmailDAO, DIAware {
    private val database: Database by instance()
    override fun save(incomingEmailDBO: IncomingEmailDBO) {
        database.save(incomingEmailDBO)
    }

    override fun fetchIncomingEmail(incomingEmailId: IncomingEmailId) =
        database.find(IncomingEmailDBO::class.java, incomingEmailId.incomingEmailId)
}
