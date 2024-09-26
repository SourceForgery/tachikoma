package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class EmailDAOImpl(override val di: DI) : EmailDAO, DIAware {
    private val database: Database by instance()

    override fun fetchEmailData(emailMessageId: EmailId) = database.find(EmailDBO::class.java, emailMessageId.emailId)

    override fun fetchEmailData(emailMessageIds: List<EmailId>): List<EmailDBO> {
        return database.find(EmailDBO::class.java)
            .where()
            .`in`("dbId", emailMessageIds.map { it.emailId })
            .findList()
    }

    override fun save(emailDBO: EmailDBO) = database.save(emailDBO)

    override fun getByEmailId(emailId: EmailId) = database.find(EmailDBO::class.java, emailId.emailId)

    override fun getByAutoMailId(autoMailId: AutoMailId) =
        database.find(EmailDBO::class.java)
            .where()
            .eq("autoMailId", autoMailId.autoMailId)
            .findOne()

    override fun getByQueueId(
        mtaQueueId: String,
        recipient: Email,
    ) = database.find(EmailDBO::class.java)
        .where()
        .eq("mtaQueueId", mtaQueueId)
        .or()
        .eq("recipient", recipient.address)
        .raw("transaction.bcc @> array[?]::text[]", recipient.address)
        .endOr()
        .orderBy()
        .desc("dateCreated")
        .setMaxRows(1)
        .findOne()
}
