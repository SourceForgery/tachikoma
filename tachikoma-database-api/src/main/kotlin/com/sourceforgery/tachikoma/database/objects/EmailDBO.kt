package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId
import com.sourceforgery.tachikoma.identifiers.MessageId
import io.ebean.annotation.DbJsonB
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

// Represents a single email to one recipient
@Entity
@Table(name = "e_email")
class EmailDBO(
    @Column
    val recipient: Email,
    @Column
    val recipientName: String,
    // Automatic handling mail id
    // Used as e.g. "unsub-[autoMailId]" and "bounce-[autoMailId]"
    @Column(unique = true)
    val autoMailId: AutoMailId,
    @ManyToOne(cascade = [CascadeType.ALL])
    val transaction: EmailSendTransactionDBO,
    @Column(unique = true)
    var messageId: MessageId,
    @Column
    var mtaQueueId: String? = null,
    @DbJsonB
    val metaData: Map<String, String>,
    // TODO should this be a string or a byte array?
    @Column(columnDefinition = "TEXT")
    var body: String? = null,
    @Column(columnDefinition = "TEXT")
    var subject: String? = null,
) : GenericDBO() {
    @OneToMany
    val emailStatusEvents: List<EmailStatusEventDBO> = ArrayList()

    constructor(
        recipient: NamedEmail,
        transaction: EmailSendTransactionDBO,
        messageId: MessageId,
        autoMailId: AutoMailId,
        mtaQueueId: String? = null,
        metaData: Map<String, String> = emptyMap(),
    ) : this(
        recipient = recipient.address,
        recipientName = recipient.name,
        transaction = transaction,
        messageId = messageId,
        autoMailId = autoMailId,
        mtaQueueId = mtaQueueId,
        metaData = metaData,
    )
}

val EmailDBO.id: EmailId
    get() = EmailId(dbId!!)

val EmailDBO.recipientNamedEmail: NamedEmail
    get() = NamedEmail(recipient, recipientName)
