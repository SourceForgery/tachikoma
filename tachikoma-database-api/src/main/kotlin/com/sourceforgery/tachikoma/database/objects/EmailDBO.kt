package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.identifiers.EmailId
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
        @Column
        var mtaQueueId: String? = null
) : GenericDBO() {

    @Column(columnDefinition = "TEXT")
            // TODO should this be a string or a byte array?
    var body: String? = null

    @OneToMany
    val emailStatusEvents: List<EmailStatusEventDBO> = BeanList()

    constructor(
            recipient: NamedEmail,
            transaction: EmailSendTransactionDBO,
            mtaQueueId: String? = null
    ) : this(
            recipient = recipient.address,
            recipientName = recipient.name,
            transaction = transaction,
            mtaQueueId = mtaQueueId
    )
}

val EmailDBO.id: EmailId
    get() = EmailId(dbId!!)
