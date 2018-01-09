package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId

interface EmailDAO {
    fun fetchEmailData(emailMessageId: EmailId): EmailDBO?
    fun fetchEmailData(emailMessageIds: List<EmailId>, sentMailMessageBodyId: SentMailMessageBodyId): MutableList<EmailDBO>
    fun save(emailDBO: EmailDBO)
    fun updateMTAQueueStatus(emailTransactionId: EmailTransactionId, queueId: String)
}