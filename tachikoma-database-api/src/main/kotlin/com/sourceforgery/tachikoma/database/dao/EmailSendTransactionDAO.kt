package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId

interface EmailSendTransactionDAO {
    fun save(emailSendTransactionDBO: EmailSendTransactionDBO)
    fun get(emailTransactionId: EmailTransactionId): EmailSendTransactionDBO?
}
