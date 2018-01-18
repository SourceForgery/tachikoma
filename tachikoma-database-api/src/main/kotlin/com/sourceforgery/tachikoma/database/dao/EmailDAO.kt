package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MessageId

interface EmailDAO {
    fun fetchEmailData(emailMessageId: EmailId): EmailDBO?
    fun fetchEmailData(emailMessageIds: List<EmailId>): List<EmailDBO>
    fun save(emailDBO: EmailDBO)
    fun updateMTAQueueStatus(emailId: EmailId, queueId: String)
    fun getByMessageId(messageId: MessageId): EmailDBO?
    fun getByQueueId(queueId: String): EmailDBO?
}
