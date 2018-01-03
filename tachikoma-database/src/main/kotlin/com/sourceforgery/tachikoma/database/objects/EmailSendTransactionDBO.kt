package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import io.ebean.common.BeanList

// Represents one call to MailDelivery.sendEmail()
class EmailSendTransactionDBO
constructor(
        val jsonRequest: String
) : GenericDBO() {
    val emails: List<EmailDBO> = BeanList()
}

val EmailSendTransactionDBO.id: EmailTransactionId
    get() = EmailTransactionId(realId as Long)