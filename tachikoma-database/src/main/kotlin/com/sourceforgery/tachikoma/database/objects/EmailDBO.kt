package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email

class EmailDBO
constructor(
        val recipient: Email,
        val recipientName: String,
        val transaction: EmailSendTransactionDBO,
        val sentEmailMessageBodyDBO: SentMailMessageBodyDBO
) : GenericDBO()
