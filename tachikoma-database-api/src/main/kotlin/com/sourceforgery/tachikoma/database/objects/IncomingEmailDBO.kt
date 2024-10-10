package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import io.ebean.annotation.DbJsonB
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Table(name = "e_incoming_email")
@Entity
class IncomingEmailDBO(
    @Column
    val mailFrom: Email,
    @Column
    val recipient: Email,
    // Temporarily writable properties
    @DbJsonB
    @Column
    var fromEmails: List<NamedEmail>,
    @DbJsonB
    @Column
    var replyToEmails: List<NamedEmail>,
    @DbJsonB
    @Column
    var toEmails: List<NamedEmail>,
    @Column
    val body: ByteArray,
    @ManyToOne
    val account: AccountDBO,
    @Column
    val subject: String,
) : GenericDBO()

val IncomingEmailDBO.id: IncomingEmailId
    get() = IncomingEmailId(dbId!!)
