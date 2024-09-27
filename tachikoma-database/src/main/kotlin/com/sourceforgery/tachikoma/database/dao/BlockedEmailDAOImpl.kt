package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.BlockedEmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.query.QBlockedEmailDBO
import com.sourceforgery.tachikoma.grpc.frontend.toMQGrpc
import com.sourceforgery.tachikoma.invoke
import com.sourceforgery.tachikoma.mq.BlockedEmailAddressEvent
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.onlyIf
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class BlockedEmailDAOImpl(override val di: DI) : BlockedEmailDAO, DIAware {
    private val database: Database by instance()
    private val mqSender: MQSender by instance()

    override fun getBlockedReason(
        accountDBO: AccountDBO,
        from: Email,
        recipient: Email,
    ): BlockedReason? {
        return QBlockedEmailDBO(database)
            .account.eq(accountDBO)
            .fromEmail.eq(from)
            .recipientEmail.eq(recipient)
            .findOne()
            ?.blockedReason
    }

    override fun block(statusEvent: EmailStatusEventDBO) {
        val from = statusEvent.email.transaction.fromEmail
        val recipient = statusEvent.email.recipient
        val account = statusEvent.email.transaction.authentication.account
        val oldBlockReason = getBlockedReason(account, from, recipient)
        if (oldBlockReason == null) {
            val blockedReason = toBlockedReason(statusEvent.emailStatus)
            val blockedEmail =
                BlockedEmailDBO(
                    recipientEmail = recipient,
                    fromEmail = from,
                    blockedReason = blockedReason,
                    account = account,
                )
            database.save(blockedEmail)
            mqSender.queueEmailBlockingNotification(
                accountId = account.id,
                (BlockedEmailAddressEvent.newBuilder()) {
                    fromEmail = from.address
                    recipientEmail = recipient.address
                    newReason = blockedReason.toMQGrpc()
                }.build(),
            )
        }
    }

    override fun unblock(
        accountDBO: AccountDBO,
        from: Email?,
        recipient: Email,
    ) {
        val unblocked =
            QBlockedEmailDBO(database)
                .onlyIf(from != null) {
                    fromEmail.eq(from)
                }
                .account.eq(accountDBO)
                .recipientEmail.eq(recipient)
                .findList()
        QBlockedEmailDBO(database)
            .onlyIf(from != null) {
                fromEmail.eq(from)
            }
            .account.eq(accountDBO)
            .recipientEmail.eq(recipient)
            .delete()

        for (blockedEmailDBO in unblocked) {
            mqSender.queueEmailBlockingNotification(
                accountId = accountDBO.id,
                (BlockedEmailAddressEvent.newBuilder()) {
                    fromEmail = blockedEmailDBO.fromEmail.address
                    recipientEmail = blockedEmailDBO.recipientEmail.address
                    newReason = com.sourceforgery.tachikoma.mq.BlockedReason.UNBLOCKED
                    oldReason = blockedEmailDBO.blockedReason.toMQGrpc()
                }.build(),
            )
        }
    }

    override fun getBlockedEmails(accountDBO: AccountDBO): List<BlockedEmailDBO> =
        QBlockedEmailDBO(database)
            .account.eq(accountDBO)
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
