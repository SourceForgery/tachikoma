package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class EmailSendTransactionDAOImpl(override val di: DI) : EmailSendTransactionDAO, DIAware {
    private val database: Database by instance()
    override fun save(emailSendTransactionDBO: EmailSendTransactionDBO) =
        database.save(emailSendTransactionDBO)

    override fun get(emailTransactionId: EmailTransactionId): EmailSendTransactionDBO? =
        database.find(EmailSendTransactionDBO::class.java, emailTransactionId.emailTransactionId)
}
