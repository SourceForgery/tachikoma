package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO

interface BlockedEmailDAO {
    fun isBlocked(from: Email, recipient: Email): Boolean
    fun block(statusEvent: EmailStatusEventDBO)
    fun unblock(statusEventDBO: EmailStatusEventDBO)
}