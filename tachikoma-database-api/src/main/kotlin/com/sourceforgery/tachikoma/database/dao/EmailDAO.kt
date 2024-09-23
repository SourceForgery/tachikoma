package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId

interface EmailDAO {
    fun fetchEmailData(emailMessageId: EmailId): EmailDBO?

    fun fetchEmailData(emailMessageIds: List<EmailId>): List<EmailDBO>

    fun save(emailDBO: EmailDBO)

    fun getByEmailId(emailId: EmailId): EmailDBO?

    fun getByAutoMailId(autoMailId: AutoMailId): EmailDBO?

    fun getByQueueId(
        mtaQueueId: String,
        recipient: Email,
    ): EmailDBO?
}
