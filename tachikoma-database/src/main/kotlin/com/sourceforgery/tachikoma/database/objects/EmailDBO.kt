package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail

class EmailDBO
constructor(
        val recipient: Email,
        val recipientName: String,
        val transaction: EmailSendTransactionDBO,
        val sentEmailMessageBodyDBO: SentMailMessageBodyDBO
) : GenericDBO() {
    constructor(
            recipient: NamedEmail,
            transaction: EmailSendTransactionDBO,
            sentEmailMessageBodyDBO: SentMailMessageBodyDBO
    ) : this(
            recipient = recipient.address,
            recipientName = recipient.name,
            transaction = transaction,
            sentEmailMessageBodyDBO = sentEmailMessageBodyDBO
    )
}
