package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import java.time.Instant

interface EmailStatusEventDAO {
    fun save(emailStatusEventDBO: EmailStatusEventDBO)
    fun getEvents(
            accountId: AccountId,
            instant: Instant? = null,
            recipientEmail: Email? = null,
            fromEmail: Email? = null,
            events: List<EmailStatus> = emptyList()
    ): List<EmailStatusEventDBO>
}