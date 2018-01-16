package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_incoming_email_address")
@Entity
class IncomingEmailAddressDBO(
        @Column(unique = true)
        val email: Email,
        @ManyToOne
        val account: AccountDBO
) : GenericDBO()