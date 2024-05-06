package com.sourceforgery.tachikoma.database.objects

import com.fasterxml.jackson.databind.node.ObjectNode
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import io.ebean.annotation.DbArray
import io.ebean.annotation.DbJsonB
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Table(name = "e_email_send_transaction")
@Entity
// Represents one call to MailDelivery.sendEmail()
class EmailSendTransactionDBO(
    // Jsonified version of the gRPC coming in through the front end
    // for logging (in JSON because of readability and searching)
    @DbJsonB
    val jsonRequest: ObjectNode,
    @Column
    val fromEmail: Email,
    @ManyToOne(cascade = [CascadeType.ALL])
    val authentication: AuthenticationDBO,
    @DbArray
    val bcc: List<String> = emptyList(),
    @DbJsonB
    val metaData: Map<String, String> = emptyMap(),
    @DbArray
    val tags: Set<String> = emptySet()

) : GenericDBO() {
    @OneToMany(cascade = [CascadeType.ALL])
    val emails: List<EmailDBO> = ArrayList()
}

val EmailSendTransactionDBO.id: EmailTransactionId
    get() = EmailTransactionId(dbId!!)
