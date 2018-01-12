package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.query.QEmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : EmailDAO {
    override fun fetchEmailData(emailMessageId: EmailId) =
            ebeanServer.find(EmailDBO::class.java, emailMessageId.emailId)

    override fun fetchEmailData(emailMessageIds: List<EmailId>): List<EmailDBO> {
        val query = QEmailDBO(ebeanServer)
        query.dbId.`in`(*emailMessageIds.map { it.emailId }.toTypedArray())
        return query.findList()
    }

    override fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)

    override fun updateMTAQueueStatus(emailId: EmailId, queueId: String) {
        ebeanServer.update(EmailDBO::class.java)
                .set("mtaQueueId", queueId)
                .where()
                .eq("dbId", emailId.emailId)
                .update()
    }
}
