package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO

interface IncomingEmailDAO {
    fun save(incomingEmailDBO: IncomingEmailDBO)
}