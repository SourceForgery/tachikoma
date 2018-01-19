package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MessageId
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
        return ebeanServer.find(EmailDBO::class.java)
                .where()
                .`in`("dbId", emailMessageIds.map { it.emailId })
                .findList()
    }

    override fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)

    override fun updateMTAQueueStatus(emailId: EmailId, queueId: String) {
        ebeanServer.update(EmailDBO::class.java)
                .set("mtaQueueId", queueId)
                .where()
                .eq("dbId", emailId.emailId)
                .update()
    }

    override fun getByMessageId(messageId: MessageId): EmailDBO? {
        return ebeanServer.find(EmailDBO::class.java)
                .where()
                .eq("messageId", messageId.messageId)
                .findOne()
    }

    override fun getByQueueId(queueId: String) =
            ebeanServer.find(EmailDBO::class.java)
                    .where()
                    .eq("mtaQueueId", queueId)
                    .findOne()
}
