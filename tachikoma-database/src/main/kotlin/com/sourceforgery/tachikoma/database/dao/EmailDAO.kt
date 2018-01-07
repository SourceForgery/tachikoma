package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.EmailId
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailDAO
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) {
    fun fetchEmailData(emailMessageId: EmailId) =
            ebeanServer.find(EmailDBO::class.java, emailMessageId.emailId)

    fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)
}