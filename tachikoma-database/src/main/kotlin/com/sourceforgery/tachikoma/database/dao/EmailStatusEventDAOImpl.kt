package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailStatusEventDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : EmailStatusEventDAO {
    override fun save(emailStatusEventDBO: EmailStatusEventDBO) = ebeanServer.save(emailStatusEventDBO)
}
