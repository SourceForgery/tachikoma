package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MessageId
import io.ebean.annotation.DbJsonB
import io.ebean.common.BeanList
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "e_email")
// Represents a single email to one recipient
class EmailDBO
constructor(
        @Column
        val recipient: Email,
        @Column
        val recipientName: String,
        @ManyToOne(cascade = [CascadeType.ALL])
        val transaction: EmailSendTransactionDBO,
        @Column(unique = true)
        var messageId: MessageId,
        @Column
        var mtaQueueId: String? = null,
        @DbJsonB
        val metaData: Map<String, String>
) : GenericDBO() {

    @Column(columnDefinition = "TEXT")
            // TODO should this be a string or a byte array?
    var body: String? = null

    @OneToMany
    val emailStatusEvents: List<EmailStatusEventDBO> = BeanList()

    constructor(
            recipient: NamedEmail,
            transaction: EmailSendTransactionDBO,
            messageId: MessageId,
            mtaQueueId: String? = null,
            metaData: Map<String, String> = HashMap()
    ) : this(
            recipient = recipient.address,
            recipientName = recipient.name,
            transaction = transaction,
            messageId = messageId,
            mtaQueueId = mtaQueueId,
            metaData = metaData
    )
}

val EmailDBO.id: EmailId
    get() = EmailId(dbId!!)
