package com.sourceforgery.tachikoma.database.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.TestBinder
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
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
import kotlin.test.assertNotNull
import org.apache.commons.lang3.RandomStringUtils
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

@RunWith(JUnitPlatform::class)
internal class EmailDAOSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var emailDAO: EmailDAO
    lateinit var dbObjectMapper: DBObjectMapper
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(RandomStringUtils.randomAlphanumeric(10), TestBinder())
        emailDAO = serviceLocator.getService(EmailDAO::class.java)
        dbObjectMapper = serviceLocator.getService(DBObjectMapper::class.java)
    }

    afterEachTest {
        serviceLocator.shutdown()
    }

    val PRINTER = JsonFormat.printer()!!

    fun getEmailDBO(from: Email, recipient: Email): EmailDBO {
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
            tags = emptyList()
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

    describe("EmailDAO") {

        it("should be possible to retrieve a saved e-mail") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val emailDBO = getEmailDBO(from, recipient)

            emailDAO.save(emailDBO)

            val savedEmail = emailDAO.fetchEmailData(emailDBO.id)

            assertNotNull(savedEmail)
        }
    }
})
