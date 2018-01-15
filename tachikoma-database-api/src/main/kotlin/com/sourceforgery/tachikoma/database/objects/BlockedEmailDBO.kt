package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.identifiers.BlockedEmailId
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.UniqueConstraint

@Entity
@Table(name = "e_blocked_email")
@UniqueConstraint(columnNames = ["from", "recipient"])
class BlockedEmailDBO
constructor(
        @Column
        val from: Email,
        @Column
        val recipient: Email,
        @Column
        val blockedReason: BlockedReason
) : GenericDBO()

val BlockedEmailDBO.id: BlockedEmailId
    get() = BlockedEmailId(dbId!!)
