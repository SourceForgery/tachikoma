package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import java.time.Instant

interface EmailStatusEventDAO {
    fun save(emailStatusEventDBO: EmailStatusEventDBO)
    fun getEventsAfter(accountId: AccountId, instant: Instant): List<EmailStatusEventDBO>
}