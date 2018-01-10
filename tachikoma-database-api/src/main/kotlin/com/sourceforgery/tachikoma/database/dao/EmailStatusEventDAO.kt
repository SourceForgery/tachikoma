package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO

interface EmailStatusEventDAO {
    fun save(emailStatusEventDBO: EmailStatusEventDBO)
}