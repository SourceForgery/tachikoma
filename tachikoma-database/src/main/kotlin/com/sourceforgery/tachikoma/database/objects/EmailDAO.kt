package com.sourceforgery.tachikoma.database.objects

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
}