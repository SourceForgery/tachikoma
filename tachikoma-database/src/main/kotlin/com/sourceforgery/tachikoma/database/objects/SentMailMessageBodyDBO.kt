package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.SentMailMessageBodyId
import io.ebean.common.BeanList
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_sent_mail_message_body")
@Entity
class SentMailMessageBodyDBO(
        @Column(columnDefinition = "TEXT")
        // TODO should this be a string or a byte array?
        val body: String
) : GenericDBO() {
    @ManyToOne(cascade = [CascadeType.ALL])
    val emails: List<EmailDBO> = BeanList()
}

val SentMailMessageBodyDBO.id: SentMailMessageBodyId
    get() = SentMailMessageBodyId(dbId!!)