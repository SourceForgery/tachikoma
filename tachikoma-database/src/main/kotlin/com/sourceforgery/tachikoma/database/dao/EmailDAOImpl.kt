package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.query.QEmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : EmailDAO {
    override fun fetchEmailData(emailMessageId: EmailId) =
            ebeanServer.find(EmailDBO::class.java, emailMessageId.emailId)

    override fun fetchEmailData(emailMessageIds: List<EmailId>, sentMailMessageBodyId: SentMailMessageBodyId): List<EmailDBO> {
        val query = QEmailDBO(ebeanServer)
        query.sentMailMessageBody.dbId.eq(sentMailMessageBodyId.sentMailMessageBodyId)
        query.dbId.`in`(*emailMessageIds.map { it.emailId }.toTypedArray())
        return query.findList()
    }

    override fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)

    override fun updateMTAQueueStatus(emailTransactionId: EmailTransactionId, queueId: String) {
        ebeanServer.update(EmailDBO::class.java)
                .set("mtaQueueId", queueId)
                .where()
                .eq("transaction.dbId", emailTransactionId.emailTransactionId)
                .update()
    }
}