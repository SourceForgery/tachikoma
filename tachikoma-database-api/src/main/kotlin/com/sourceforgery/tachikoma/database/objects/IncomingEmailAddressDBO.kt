package com.sourceforgery.tachikoma.database.objects

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Table(name = "e_incoming_email_address", uniqueConstraints = [
    UniqueConstraint(columnNames = ["local_part", "account_id"])
])
@Entity
class IncomingEmailAddressDBO(
        @Column
        val localPart: String,

        @ManyToOne
        val account: AccountDBO
) : GenericDBO()