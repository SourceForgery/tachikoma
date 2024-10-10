package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.BlockedEmailId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "e_blocked_email")
@UniqueConstraint(columnNames = ["from_email", "recipient_email"])
class BlockedEmailDBO
    constructor(
        @Column
        val fromEmail: Email,
        @Column
        val recipientEmail: Email,
        @Column
        val blockedReason: BlockedReason,
        @ManyToOne
        val account: AccountDBO,
    ) : GenericDBO()

val BlockedEmailDBO.id: BlockedEmailId
    get() = BlockedEmailId(dbId!!)
