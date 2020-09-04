package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSearchFilterQuery
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import kotlinx.coroutines.flow.Flow

interface IncomingEmailDAO {
    fun save(incomingEmailDBO: IncomingEmailDBO)
    fun fetchIncomingEmail(incomingEmailId: IncomingEmailId, accountId: AccountId): IncomingEmailDBO?
    fun searchIncomingEmails(accountId: AccountId, filter: List<EmailSearchFilterQuery>): Flow<IncomingEmailDBO>
}
