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
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
internal class EmailStatusEventDAOSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var emailDAO: EmailDAO
    lateinit var emailStatusEventDAO: EmailStatusEventDAO
    lateinit var accountDAO: AccountDAO
    lateinit var authenticationDAO: AuthenticationDAO
    lateinit var dbObjectMapper: DBObjectMapper
    lateinit var clock: Clock
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder())
        emailDAO = serviceLocator.getService(EmailDAO::class.java)
        emailStatusEventDAO = serviceLocator.getService(EmailStatusEventDAO::class.java)
        accountDAO = serviceLocator.getService(AccountDAO::class.java)
        authenticationDAO = serviceLocator.getService(AuthenticationDAO::class.java)
        dbObjectMapper = serviceLocator.getService(DBObjectMapper::class.java)
        clock = serviceLocator.getService(Clock::class.java)
    }

    afterEachTest {
        serviceLocator.shutdown()
    }

    val PRINTER = JsonFormat.printer()!!

    fun createAuthentication(domain: String): AuthenticationDBO {
        val accountDBO = AccountDBO(MailDomain(domain))
        accountDAO.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
                username = domain,
                encryptedPassword = UUID.randomUUID().toString(),
                apiToken = UUID.randomUUID().toString(),
                role = AuthenticationRole.BACKEND,
                account = accountDBO
        )
        authenticationDAO.save(authenticationDBO)

        return authenticationDBO
    }

    fun createEmailStatusEvent(
            authentication: AuthenticationDBO,
            from: Email,
            recipient: Email,
            emailStatus: EmailStatus,
            dateCreated: Instant? = null
    ): EmailStatusEventDBO {

        val outgoingEmail = OutgoingEmail.newBuilder().build()
        val jsonRequest = dbObjectMapper.readValue(PRINTER.print(outgoingEmail)!!, ObjectNode::class.java)!!

        val email = EmailDBO(
                recipient = recipient,
                recipientName = "Mr. Recipient",
                transaction = EmailSendTransactionDBO(
                        jsonRequest = jsonRequest,
                        fromEmail = from,
                        authentication = authentication
                ),
                messageId = MessageId(UUID.randomUUID().toString()),
                mtaQueueId = null
        )
        emailDAO.save(email)

        val emailStatusEventDBO = EmailStatusEventDBO(
                emailStatus = emailStatus,
                email = email,
                metaData = StatusEventMetaData()
        )
        emailStatusEventDAO.save(emailStatusEventDBO)
        dateCreated ?. also {
            emailStatusEventDBO.dateCreated = it
            emailStatusEventDAO.save(emailStatusEventDBO)
        }

        return emailStatusEventDBO
    }

    describe("EmailStatusEventDAO") {

        it("it should return all recent status events for specified account") {

            val authentication1 = createAuthentication("example.org")
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient1@example.org"),
                    emailStatus = EmailStatus.DELIVERED
            )
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient2@example.org"),
                    emailStatus = EmailStatus.DELIVERED
            )
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient3@example.org"),
                    emailStatus = EmailStatus.DELIVERED,
                    dateCreated = clock.instant().minus(3, ChronoUnit.DAYS)
            )

            val authentication2 = createAuthentication("example.com")
            createEmailStatusEvent(
                    authentication = authentication2,
                    from = Email("from@example.com"),
                    recipient = Email("recipient@example.com"),
                    emailStatus = EmailStatus.DELIVERED
            )

            val eventsTimeLimit = clock.instant().minus(2, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEventsAfter(authentication1.account.id, eventsTimeLimit)

            assertEquals(2, emailStatusEvents.size)
            assertEquals("recipient1@example.org", emailStatusEvents[0].email.recipient.address)
            assertEquals("recipient2@example.org", emailStatusEvents[1].email.recipient.address)
        }

        it("it should not return events outside time limit") {

            val authentication1 = createAuthentication("example.org")
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient1@example.org"),
                    emailStatus = EmailStatus.DELIVERED
            )

            val eventsTimeLimit = clock.instant().plus(2, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEventsAfter(authentication1.account.id, eventsTimeLimit)

            assertEquals(0, emailStatusEvents.size)
        }
    }
})