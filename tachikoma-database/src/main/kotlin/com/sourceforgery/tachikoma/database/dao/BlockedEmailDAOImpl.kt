package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.BlockedEmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class BlockedEmailDAOImpl
@Inject
private constructor(
    private val ebeanServer: EbeanServer
) : BlockedEmailDAO {
    override fun getBlockedReason(accountDBO: AccountDBO, from: Email, recipient: Email): BlockedReason? {
        return ebeanServer.find(BlockedEmailDBO::class.java)
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
            val blockedEmail = BlockedEmailDBO(
                recipientEmail = recipient,
                fromEmail = from,
                blockedReason = toBlockedReason(statusEvent.emailStatus),
                account = account
            )
            ebeanServer.save(blockedEmail)
        }
    }

    override fun unblock(statusEventDBO: EmailStatusEventDBO) {
        ebeanServer
            .find(BlockedEmailDBO::class.java)
            .where()
            .eq("account", statusEventDBO.email.transaction.authentication.account)
            .eq("fromEmail", statusEventDBO.email.transaction.fromEmail)
            .eq("recipientEmail", statusEventDBO.email.recipient)
            .delete()
    }

    override fun unblock(accountDBO: AccountDBO, from: Email?, recipient: Email) {
        ebeanServer
            .find(BlockedEmailDBO::class.java)
            .where()
            .raw("fromEmail = ? IS NOT FALSE", from)
            .eq("account", accountDBO)
            .eq("recipientEmail", recipient)
            .delete()
    }

    override fun getBlockedEmails(accountDBO: AccountDBO): List<BlockedEmailDBO> =
        ebeanServer
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