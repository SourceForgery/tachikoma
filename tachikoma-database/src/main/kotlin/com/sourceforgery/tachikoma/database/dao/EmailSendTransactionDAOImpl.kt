package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailSendTransactionDAOImpl
@Inject
private constructor(
    private val ebeanServer: EbeanServer
) : EmailSendTransactionDAO {
    override fun save(emailSendTransactionDBO: EmailSendTransactionDBO) =
        ebeanServer.save(emailSendTransactionDBO)
}
