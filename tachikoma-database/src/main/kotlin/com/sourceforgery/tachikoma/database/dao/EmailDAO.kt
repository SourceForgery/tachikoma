package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailDAO
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) {
    fun fetchEmailData(emailMessageId: EmailId) =
            ebeanServer.find(EmailDBO::class.java, emailMessageId.emailId)

    fun fetchEmailData(emailMessageIds: List<EmailId>, sentMailMessageBodyId: SentMailMessageBodyId) =
            ebeanServer.find(EmailDBO::class.java)
                    .where()
                    .eq("sentMailMessageBody.dbId", sentMailMessageBodyId.sentMailMessageBodyId)
                    .`in`("dbId", emailMessageIds.map { it.emailId })
                    .findList()

    fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)

    fun updateMTAQueueStatus(emailTransactionId: EmailTransactionId, queueId: String) {
        ebeanServer.update(EmailDBO::class.java)
                .set("mtaQueueId", queueId)
                .where()
                .eq("transaction.dbId", emailTransactionId.emailTransactionId)
                .update()
    }
}