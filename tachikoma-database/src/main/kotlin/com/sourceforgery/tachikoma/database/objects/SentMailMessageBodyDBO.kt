package com.sourceforgery.tachikoma.database.objects

import io.ebean.common.BeanList
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_sent_mail_message_body")
@Entity
class SentMailMessageBodyDBO(
        @Column
        val body: String
) : GenericDBO() {
    @ManyToOne
    val emails: List<EmailDBO> = BeanList()
}