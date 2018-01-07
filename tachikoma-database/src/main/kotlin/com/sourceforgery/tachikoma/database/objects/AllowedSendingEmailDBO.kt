package com.sourceforgery.tachikoma.database.objects

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

@Table(name = "e_allowed_sending_email")
@Entity
class AllowedSendingEmailDBO(
        @Column
        val domainPart: String,
        @Column
        val localPart: String? = null,
        @ManyToOne
        val account: AccountDBO
) : GenericDBO()