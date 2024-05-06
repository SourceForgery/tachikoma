package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.query.QEmailStatusEventDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.time.Instant

class EmailStatusEventDAOImpl(override val di: DI) : EmailStatusEventDAO, DIAware {
    private val database: Database by instance()

    override fun save(emailStatusEventDBO: EmailStatusEventDBO) = database.save(emailStatusEventDBO)

    override fun getEvents(
        accountId: AccountId,
        instant: Instant?,
        recipientEmail: Email?,
        fromEmail: Email?,
        events: List<EmailStatus>,
        tags: Set<String>,
    ): List<EmailStatusEventDBO> {
        return QEmailStatusEventDBO(database)
            .email.transaction.authentication.account.dbId.eq(accountId.accountId)
            .apply {
                if (instant != null) {
                    dateCreated.gt(instant)
                }
                if (recipientEmail != null) {
                    email.recipient.eq(recipientEmail)
                }
                if (fromEmail != null) {
                    email.transaction.fromEmail.eq(fromEmail)
                }
                if (events.isNotEmpty()) {
                    emailStatus.`in`(events)
                }
                rawOrEmpty("email.transaction.tags && ARRAY[?1]::text[]", tags)
            }
            .orderBy()
            .dateCreated.asc()
            .findList()
    }
}
