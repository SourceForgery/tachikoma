package com.sourceforgery.tachikoma.database.objects

import com.fasterxml.jackson.databind.node.ObjectNode
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.EmailTransactionId
import io.ebean.annotation.DbArray
import io.ebean.annotation.DbJsonB
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

// Represents one call to MailDelivery.sendEmail()
@Table(name = "e_email_send_transaction")
@Entity
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
    val tags: Set<String> = emptySet(),
) : GenericDBO() {
    @OneToMany(cascade = [CascadeType.ALL])
    val emails: List<EmailDBO> = ArrayList()
}

val EmailSendTransactionDBO.id: EmailTransactionId
    get() = EmailTransactionId(dbId!!)
