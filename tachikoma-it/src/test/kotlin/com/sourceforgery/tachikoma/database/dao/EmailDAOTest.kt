package com.sourceforgery.tachikoma.database.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.TestAttribute
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.database.find
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmailDAOTest : DIAware {
    override val di = DI {
        importOnce(testModule(TestAttribute.POSTGRESQL), allowOverride = true)
    }

    val emailDAO: EmailDAO by instance()
    val dbObjectMapper: DBObjectMapper by instance()
    val database: Database by instance()

    val PRINTER = JsonFormat.printer()!!

    @Before
    fun b4() {
        database.find<EmailDBO>().delete()
    }

    fun getEmailDBO(from: Email, recipient: Email, bcc: List<Email> = emptyList()): EmailDBO {
        val account = AccountDBO(MailDomain("example.com"))
        val authentication = AuthenticationDBO(
            encryptedPassword = null,
            apiToken = null,
            role = AuthenticationRole.BACKEND,
            account = account
        )

        val outgoingEmail = OutgoingEmail.newBuilder().build()
        val jsonRequest = dbObjectMapper.objectMapper.readValue(PRINTER.print(outgoingEmail)!!, ObjectNode::class.java)!!

        val emailSendTransaction = EmailSendTransactionDBO(
            jsonRequest = jsonRequest,
            fromEmail = from,
            authentication = authentication,
            metaData = emptyMap(),
            tags = emptySet(),
            bcc = bcc.map { it.address }
        )

        return EmailDBO(
            recipient = recipient,
            recipientName = "Mr. Recipient",
            transaction = emailSendTransaction,
            messageId = MessageId("1023@example.com"),
            autoMailId = AutoMailId("1023@example.net"),
            mtaQueueId = null,
            metaData = emptyMap()
        )
    }

    @Test
    fun `should be possible to retrieve a saved e-mail`() {

        val from = Email("from@example.com")
        val recipient = Email("recipient@example.com")

        val emailDBO = getEmailDBO(from, recipient)

        emailDAO.save(emailDBO)

        val savedEmail = emailDAO.fetchEmailData(emailDBO.id)

        assertNotNull(savedEmail)
    }

    @Test
    fun `should be possible to retrieve an email by mtaQueueId and email`() {
        val from = Email("from@example.com")
        val recipient = Email("recipient@example.com")
        val bccRecipient = Email("bcc_recipient@example.com")

        val emailDBO = getEmailDBO(from, recipient, listOf(bccRecipient))
        val mtaQueueId = RandomStringUtils.randomAlphanumeric(10)
        emailDBO.mtaQueueId = mtaQueueId

        assertNull(emailDAO.getByQueueId(mtaQueueId, recipient))
        emailDAO.save(emailDBO)

        assertNull(emailDAO.getByQueueId(mtaQueueId, from))

        val savedEmail = emailDAO.getByQueueId(mtaQueueId, bccRecipient)
        assertNotNull(savedEmail)
    }
}
