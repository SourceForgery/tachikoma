package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO

interface EmailSendTransactionDAO {
    fun save(emailSendTransactionDBO: EmailSendTransactionDBO)
}
