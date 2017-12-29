package com.sourceforgery.tachikoma.database.objects

import io.ebean.common.BeanList

// Represents one call to MailDelivery.sendEmail()
class EmailSendTransactionDBO
constructor(
        val jsonRequest: String
) : GenericDBO() {
    val emails: List<EmailDBO> = BeanList()
}