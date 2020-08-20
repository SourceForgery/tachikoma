package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.EmailId
import io.ebean.EbeanServer
import javax.inject.Inject

class EmailDAOImpl
@Inject
private constructor(
    private val ebeanServer: EbeanServer
) : EmailDAO {
    override fun fetchEmailData(emailMessageId: EmailId) =
        ebeanServer.find(EmailDBO::class.java, emailMessageId.emailId)

    override fun fetchEmailData(emailMessageIds: List<EmailId>): List<EmailDBO> {
        return ebeanServer.find(EmailDBO::class.java)
            .where()
            .`in`("dbId", emailMessageIds.map { it.emailId })
            .findList()
    }

    override fun save(emailDBO: EmailDBO) = ebeanServer.save(emailDBO)

    override fun getByEmailId(emailId: EmailId) =
        ebeanServer.find(EmailDBO::class.java, emailId.emailId)

    override fun getByAutoMailId(autoMailId: AutoMailId) =
        ebeanServer.find(EmailDBO::class.java)
            .where()
            .eq("autoMailId", autoMailId.autoMailId)
            .findOne()

    override fun getByQueueId(mtaQueueId: String, recipient: Email) =
        ebeanServer.find(EmailDBO::class.java)
            .where()
            .eq("mtaQueueId", mtaQueueId)
            .eq("recipient", recipient.address)
            .orderBy()
            .desc("dateCreated")
            .setMaxRows(1)
            .findOne()
}
