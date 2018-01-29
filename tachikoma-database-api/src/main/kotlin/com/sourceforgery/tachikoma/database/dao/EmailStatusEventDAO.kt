package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import java.time.Instant

interface EmailStatusEventDAO {
    fun save(emailStatusEventDBO: EmailStatusEventDBO)
    fun getEventsAfter(accountDBO: AccountDBO, instant: Instant): List<EmailStatusEventDBO>
}