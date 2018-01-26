package com.sourceforgery.tachikoma.database.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import io.ebean.EbeanServer
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
internal class EmailStatusEventDAOSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var emailStatusEventDAO: EmailStatusEventDAO
    lateinit var accountDAO: AccountDAO
    lateinit var dbObjectMapper: DBObjectMapper
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder())
        emailStatusEventDAO = serviceLocator.getService(EmailStatusEventDAO::class.java)
        accountDAO = serviceLocator.getService(AccountDAO::class.java)
        dbObjectMapper = serviceLocator.getService(DBObjectMapper::class.java)
    }

    afterEachTest {
        serviceLocator.shutdown()
    }

    val PRINTER = JsonFormat.printer()!!

    fun getEmailStatusEvent(accountDBO: AccountDBO, from: Email, recipient: Email, emailStatus: EmailStatus): EmailStatusEventDBO {
        val authentication = AuthenticationDBO(
                encryptedPassword = null,
                apiToken = null,
                role = AuthenticationRole.BACKEND,
                account = accountDBO
        )

        val outgoingEmail = OutgoingEmail.newBuilder().build()
        val jsonRequest = dbObjectMapper.readValue(PRINTER.print(outgoingEmail)!!, ObjectNode::class.java)!!

        val emailSendTransaction = EmailSendTransactionDBO(
                jsonRequest = jsonRequest,
                fromEmail = from,
                authentication = authentication
        )

        val fromEmail = EmailDBO(
                recipient = recipient,
                recipientName = "Mr. Recipient",
                transaction = emailSendTransaction,
                messageId = MessageId("1023"),
                mtaQueueId = null
        )

        return EmailStatusEventDBO(
                emailStatus = emailStatus,
                email = fromEmail,
                mtaStatusCode = null
        )
    }

    describe("EmailStatusEventDAO") {

        var accountDBO: AccountDBO? = null

        beforeEachTest {
            accountDBO = AccountDBO(MailDomain("example.com"))
            accountDAO.save(accountDBO!!)
        }

        afterEachTest {
            serviceLocator
                    .getServiceHandle(EbeanServer::class.java)
                    .destroy()
        }

        fun getAccount(): AccountDBO {
            return accountDBO!!
        }

        it("it should return all recent status events") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val emailStatusEvent = getEmailStatusEvent(
                    accountDBO = getAccount(),
                    from = from,
                    recipient = recipient,
                    emailStatus = EmailStatus.OPENED
            )

            emailStatusEventDAO.save(emailStatusEvent)

            val eventsTimeLimit = Instant.now().minus(2, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEventsAfter(eventsTimeLimit)

            assertEquals(1, emailStatusEvents.size)
        }

        it("it should not return events outside time limit") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val emailStatusEvent = getEmailStatusEvent(
                    accountDBO = getAccount(),
                    from = from,
                    recipient = recipient,
                    emailStatus = EmailStatus.OPENED
            )

            emailStatusEventDAO.save(emailStatusEvent)

            val eventsTimeLimit = Instant.now().plus(2, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEventsAfter(eventsTimeLimit)

            assertEquals(0, emailStatusEvents.size)
        }
    }
})