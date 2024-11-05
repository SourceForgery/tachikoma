package com.sourceforgery.tachikoma.database.objects

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Table(
    name = "e_incoming_email_address",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["local_part", "account_id"]),
    ],
)
@Entity
class IncomingEmailAddressDBO(
    @Column
    val localPart: String,
    @ManyToOne
    val account: AccountDBO,
) : GenericDBO()
