package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId

interface IncomingEmailDAO {
    fun save(incomingEmailDBO: IncomingEmailDBO)
    fun fetchIncomingEmail(incomingEmailId: IncomingEmailId, accountId: AccountId): IncomingEmailDBO?
}
