package com.sourceforgery.tachikoma.database.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.find
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.BlockedEmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.Collections
import kotlin.test.assertEquals

class BlockedEmailDAOTest : DIAware {
    override val di: DI = DI {
        importOnce(testModule(), allowOverride = true)
    }

    val mailDomain = MailDomain("example.com")
    val blockedEmailDAO: BlockedEmailDAO by instance()
    val accountDAO: AccountDAO by instance()
    val dbObjectMapper: DBObjectMapper by instance()
    val database: Database by instance()

    val PRINTER = JsonFormat.printer()!!

    @Before
    fun `create domain`() {
        accountDBO = accountDAO.getByMailDomain(mailDomain)
            ?: let {
                val a = AccountDBO(mailDomain)
                accountDAO.save(a)
                a
            }
        database.find<BlockedEmailDBO>()
            .delete()
    }

    fun getEmailStatusEvent(accountDBO: AccountDBO, from: Email, recipient: Email, emailStatus: EmailStatus): EmailStatusEventDBO {
        val authentication = AuthenticationDBO(
            encryptedPassword = null,
            apiToken = null,
            role = AuthenticationRole.BACKEND,
            account = accountDBO
        )

        val outgoingEmail = OutgoingEmail.newBuilder().build()
        val jsonRequest = dbObjectMapper.objectMapper.readValue(PRINTER.print(outgoingEmail)!!, ObjectNode::class.java)!!

        val emailSendTransaction = EmailSendTransactionDBO(
            jsonRequest = jsonRequest,
            fromEmail = from,
            authentication = authentication,
            metaData = emptyMap(),
            tags = emptyList()
        )

        val fromEmail = EmailDBO(
            recipient = recipient,
            recipientName = "Mr. Recipient",
            transaction = emailSendTransaction,
            messageId = MessageId("1023@example.com"),
            autoMailId = AutoMailId("1023@example.net"),
            mtaQueueId = null,
            metaData = emptyMap()
        )

        return EmailStatusEventDBO(
            emailStatus = emailStatus,
            email = fromEmail,
            metaData = StatusEventMetaData()
        )
    }

    var accountDBO: AccountDBO? = null

    fun getAccount(): AccountDBO {
        return accountDBO!!
    }

    fun blockEmails(recipient: Email) {
        blockedEmailDAO.block(
            getEmailStatusEvent(
                accountDBO = getAccount(),
                from = Email("from1@example.com"),
                recipient = recipient,
                emailStatus = EmailStatus.HARD_BOUNCED
            )
        )
        blockedEmailDAO.block(
            getEmailStatusEvent(
                accountDBO = getAccount(),
                from = Email("from2@example.com"),
                recipient = recipient,
                emailStatus = EmailStatus.UNSUBSCRIBE
            )
        )
        blockedEmailDAO.block(
            getEmailStatusEvent(
                accountDBO = getAccount(),
                from = Email("from3@example.com"),
                recipient = recipient,
                emailStatus = EmailStatus.UNSUBSCRIBE
            )
        )
        blockedEmailDAO.block(
            getEmailStatusEvent(
                accountDBO = getAccount(),
                from = Email("from4@example.com"),
                recipient = recipient,
                emailStatus = EmailStatus.UNSUBSCRIBE
            )
        )
    }

    @Test
    fun `should return null if e-mail is not blocked`() {
        val from = Email("from@example.com")
        val recipient = Email("recipient@example.com")

        val blockedReason = blockedEmailDAO.getBlockedReason(getAccount(), from, recipient)

        assertEquals(null, blockedReason)
    }

    @Test
    fun `should return blocked reason if e-mail is blocked`() {

        val from = Email("from@example.com")
        val recipient = Email("recipient@example.com")

        val emailStatusEvent = getEmailStatusEvent(getAccount(), from, recipient, EmailStatus.UNSUBSCRIBE)

        blockedEmailDAO.block(emailStatusEvent)

        val blockedReason = blockedEmailDAO.getBlockedReason(getAccount(), from, recipient)

        assertEquals(BlockedReason.UNSUBSCRIBED, blockedReason)
    }

    @Test
    fun `should should be possible to unblock a blocked e-mail`() {

        val from = Email("from@example.com")
        val recipient = Email("recipient@example.com")

        val emailStatusEvent = getEmailStatusEvent(getAccount(), from, recipient, EmailStatus.HARD_BOUNCED)

        blockedEmailDAO.block(emailStatusEvent)

        assertEquals(BlockedReason.HARD_BOUNCED, blockedEmailDAO.getBlockedReason(getAccount(), from, recipient))

        blockedEmailDAO.unblock(emailStatusEvent)

        assertEquals(null, blockedEmailDAO.getBlockedReason(getAccount(), from, recipient))
    }

    @Test
    fun `should should be possible to get a list of all blocked e-mails`() {

        val recipient = Email("recipient@example.com")

        blockEmails(recipient)

        assertEquals(4, blockedEmailDAO.getBlockedEmails(getAccount()).size)
    }

    @Test
    fun `should should be possible to unblock blocked e-mails by from & recipient`() {

        val recipient = Email("recipient@example.com")

        blockEmails(recipient)

        blockedEmailDAO.unblock(getAccount(), Email("from2@example.com"), recipient)

        assertEquals(3, blockedEmailDAO.getBlockedEmails(getAccount()).size)
    }

    @Test
    fun `should should be possible to unblock all blocked e-mails by recipient`() {
        val recipient = Email("recipient@example.com")

        blockEmails(recipient)

        blockedEmailDAO.unblock(getAccount(), null, recipient)

        assertEquals(Collections.EMPTY_LIST, blockedEmailDAO.getBlockedEmails(getAccount()))
    }
}
