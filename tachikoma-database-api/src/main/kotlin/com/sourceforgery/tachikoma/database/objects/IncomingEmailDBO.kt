package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_incoming_email")
@Entity
class IncomingEmailDBO(
        @Column
        val fromEmail: Email,
        @Column
        val receiverEmail: Email,
        @Column
        val body: ByteArray,
        @ManyToOne
        val accountDBO: AccountDBO
) : GenericDBO()

val IncomingEmailDBO.id: IncomingEmailId
    get() = IncomingEmailId(dbId!!)