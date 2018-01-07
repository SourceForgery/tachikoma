package com.sourceforgery.tachikoma.database.objects

import com.fasterxml.jackson.databind.node.ObjectNode
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import io.ebean.annotation.DbJsonB
import io.ebean.common.BeanList
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_email_send_transaction")
@Entity
// Represents one call to MailDelivery.sendEmail()
class EmailSendTransactionDBO
constructor(
        // Jsonified version of the gRPC coming in through the front end
        // for logging (in JSON because of readability and searching)
        @DbJsonB
        val jsonRequest: ObjectNode
) : GenericDBO() {
    @OneToMany(cascade = [CascadeType.ALL])
    val emails: List<EmailDBO> = BeanList()
}

val EmailSendTransactionDBO.id: EmailTransactionId
    get() = EmailTransactionId(realId!!)