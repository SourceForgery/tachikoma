package com.sourceforgery.tachikoma.database.objects

import com.sourceforgery.tachikoma.common.Email
import javax.persistence.Column

class IncomingEmailAddressDBO(
        @Column(unique = true)
        val email: Email,
        @Column
        val accountDBO: AccountDBO
) : GenericDBO()