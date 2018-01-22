package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.identifiers.MailDomain
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Table(name = "e_incoming_email_address", uniqueConstraints = [
    UniqueConstraint(columnNames = ["local_part", "mail_domain"])
])
@Entity
class IncomingEmailAddressDBO(
        @Column
        val localPart: String?,
        @Column
        val mailDomain: MailDomain,

        @ManyToOne
        val account: AccountDBO
) : GenericDBO()