package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Table(name = "e_incoming_email")
@Entity
class IncomingEmailDBO(
        @Column
        val fromEmail: Email,
        @Column
        val toEmail: Email,
        @Column
        val body: ByteArray
) : GenericDBO()

val IncomingEmailDBO.id: IncomingEmailId
    get() = IncomingEmailId(dbId!!)