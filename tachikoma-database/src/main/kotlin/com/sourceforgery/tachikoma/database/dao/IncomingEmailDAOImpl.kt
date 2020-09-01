package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.EmailSearchFilterQuery
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.ReceivedBetween
import com.sourceforgery.tachikoma.database.objects.ReceiverEmailContains
import com.sourceforgery.tachikoma.database.objects.ReceiverNameContains
import com.sourceforgery.tachikoma.database.objects.SenderEmailContains
import com.sourceforgery.tachikoma.database.objects.SenderNameContains
import com.sourceforgery.tachikoma.database.objects.SubjectContains
import com.sourceforgery.tachikoma.database.objects.query.QIncomingEmailDBO
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import io.ebean.Database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class IncomingEmailDAOImpl(override val di: DI) : IncomingEmailDAO, DIAware {
    private val database: Database by instance()
    override fun save(incomingEmailDBO: IncomingEmailDBO) {
        database.save(incomingEmailDBO)
    }

    override fun fetchIncomingEmail(incomingEmailId: IncomingEmailId, accountId: AccountId) =
        QIncomingEmailDBO(database)
            .account.dbId.eq(accountId.accountId)
            .dbId.eq(incomingEmailId.incomingEmailId)
            .findOne()

    override fun searchIncomingEmails(accountId: AccountId, filter: List<EmailSearchFilterQuery>): Flow<IncomingEmailDBO> {
        val q = QIncomingEmailDBO(database)
            .apply {
                for (emailSearchFilterQuery in filter) {
                    @Suppress("UNUSED_VARIABLE")
                    val allCasesCovered = when (emailSearchFilterQuery) {
                        is SubjectContains -> subject.contains(emailSearchFilterQuery.subject)
                        is SenderNameContains -> fromName.contains(emailSearchFilterQuery.name)
                        is SenderEmailContains -> raw("fromEmail LIKE ?", emailSearchFilterQuery.email)
                        is ReceiverNameContains -> receiverName.contains(emailSearchFilterQuery.name)
                        is ReceiverEmailContains -> raw("receiverEmail LIKE ?", emailSearchFilterQuery.email)
                        is ReceivedBetween -> dateCreated.between(emailSearchFilterQuery.after, emailSearchFilterQuery.before)
                    }
                }
                account.dbId.eq(accountId.accountId)
            }

        return flow {
            q.findIterate()
                .use {
                    emitAll(
                        it.asFlow()
                    )
                }
        }
    }
}
