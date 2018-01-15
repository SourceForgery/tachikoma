package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.BlockedEmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class BlockedEmailDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : BlockedEmailDAO {
    override fun isBlocked(from: Email, recipient: Email): Boolean {
        return ebeanServer.find(BlockedEmailDBO::class.java)
                .where()
                .eq("from", from)
                .eq("recipient", recipient)
                .findCount() > 0
    }

    override fun block(statusEvent: EmailStatusEventDBO) {
        val blockedEmail = BlockedEmailDBO(
                recipient = statusEvent.email.recipient,
                from = statusEvent.email.transaction.fromEmail,
                emailStatus = statusEvent.emailStatus
        )
        ebeanServer.save(blockedEmail)
    }

    override fun unblock(statusEventDBO: EmailStatusEventDBO) {
        ebeanServer.find(BlockedEmailDBO::class.java)
                .where()
                .eq("from", statusEventDBO.email.transaction.fromEmail)
                .eq("recipient", statusEventDBO.email.recipient)
                .delete()
    }
}