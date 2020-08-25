package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import io.ebean.Database
import java.time.Instant
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class EmailStatusEventDAOImpl(override val di: DI) : EmailStatusEventDAO, DIAware {
    private val database: Database by instance()

    override fun save(emailStatusEventDBO: EmailStatusEventDBO) = database.save(emailStatusEventDBO)

    override fun getEvents(
        accountId: AccountId,
        instant: Instant?,
        recipientEmail: Email?,
        fromEmail: Email?,
        events: List<EmailStatus>
    ): List<EmailStatusEventDBO> {
        return database
            .find(EmailStatusEventDBO::class.java)
            .where()
            .eq("email.transaction.authentication.account.dbId", accountId.accountId)
            .apply {
                if (instant != null) {
                    gt("dateCreated", instant)
                }
                if (recipientEmail != null) {
                    eq("email.recipient", recipientEmail)
                }
                if (fromEmail != null) {
                    eq("email.transaction.fromEmail", fromEmail)
                }
                if (events.isNotEmpty()) {
                    `in`("emailStatus", events)
                }
            }
            .findList()
    }
}
