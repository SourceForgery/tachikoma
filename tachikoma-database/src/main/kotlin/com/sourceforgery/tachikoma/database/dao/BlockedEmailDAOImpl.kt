package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.BlockedEmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class BlockedEmailDAOImpl(override val di: DI) : BlockedEmailDAO, DIAware {
    private val database: Database by instance()

    override fun getBlockedReason(
        accountDBO: AccountDBO,
        from: Email,
        recipient: Email,
    ): BlockedReason? {
        return database.find(BlockedEmailDBO::class.java)
            .where()
            .eq("account", accountDBO)
            .eq("fromEmail", from)
            .eq("recipientEmail", recipient)
            .findOne()
            ?.blockedReason
    }

    override fun block(statusEvent: EmailStatusEventDBO) {
        val from = statusEvent.email.transaction.fromEmail
        val recipient = statusEvent.email.recipient
        val account = statusEvent.email.transaction.authentication.account
        if (getBlockedReason(account, from, recipient) == null) {
            val blockedEmail =
                BlockedEmailDBO(
                    recipientEmail = recipient,
                    fromEmail = from,
                    blockedReason = toBlockedReason(statusEvent.emailStatus),
                    account = account,
                )
            database.save(blockedEmail)
        }
    }

    override fun unblock(statusEventDBO: EmailStatusEventDBO) {
        database
            .find(BlockedEmailDBO::class.java)
            .where()
            .eq("account", statusEventDBO.email.transaction.authentication.account)
            .eq("fromEmail", statusEventDBO.email.transaction.fromEmail)
            .eq("recipientEmail", statusEventDBO.email.recipient)
            .delete()
    }

    override fun unblock(
        accountDBO: AccountDBO,
        from: Email?,
        recipient: Email,
    ) {
        database
            .find(BlockedEmailDBO::class.java)
            .where()
            .raw("fromEmail = ? IS NOT FALSE", from)
            .eq("account", accountDBO)
            .eq("recipientEmail", recipient)
            .delete()
    }

    override fun getBlockedEmails(accountDBO: AccountDBO): List<BlockedEmailDBO> =
        database
            .find(BlockedEmailDBO::class.java)
            .where()
            .eq("account", accountDBO)
            .findList()

    private fun toBlockedReason(emailStatus: EmailStatus): BlockedReason {
        return when (emailStatus) {
            EmailStatus.UNSUBSCRIBE -> BlockedReason.UNSUBSCRIBED
            EmailStatus.HARD_BOUNCED -> BlockedReason.HARD_BOUNCED
            EmailStatus.SPAM -> BlockedReason.SPAM_MARKED
            else -> throw IllegalArgumentException("$emailStatus is not valid for blocking")
        }
    }
}
