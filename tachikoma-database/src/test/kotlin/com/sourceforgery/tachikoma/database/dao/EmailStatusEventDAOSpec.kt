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
import com.sourceforgery.tachikoma.hk2.get
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
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
internal class EmailStatusEventDAOSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var emailStatusEventDAO: EmailStatusEventDAO
    lateinit var dbObjectMapper: DBObjectMapper
    lateinit var ebeanServer: EbeanServer
    lateinit var clock: Clock
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder())
        emailStatusEventDAO = serviceLocator.get()
        dbObjectMapper = serviceLocator.get()
        ebeanServer = serviceLocator.get()
        clock = serviceLocator.get()
    }

    afterEachTest {
        serviceLocator.shutdown()
    }

    val PRINTER = JsonFormat.printer()!!

    fun createAuthentication(domain: String): AuthenticationDBO {
        val accountDBO = AccountDBO(MailDomain(domain))
        ebeanServer.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
                login = domain,
                encryptedPassword = UUID.randomUUID().toString(),
                apiToken = UUID.randomUUID().toString(),
                role = AuthenticationRole.BACKEND,
                account = accountDBO
        )
        ebeanServer.save(authenticationDBO)

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
                        authentication = authentication,
                        metaData = emptyMap(),
                        tags = emptyList()
                ),
                messageId = MessageId(UUID.randomUUID().toString()),
                mtaQueueId = null,
                metaData = emptyMap()
        )
        ebeanServer.save(email)

        val emailStatusEventDBO = EmailStatusEventDBO(
                emailStatus = emailStatus,
                email = email,
                metaData = StatusEventMetaData()
        )
        ebeanServer.save(emailStatusEventDBO)
        dateCreated?.also {
            emailStatusEventDBO.dateCreated = it
            ebeanServer.save(emailStatusEventDBO)
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
            val event1 = createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient3@example.org"),
                    emailStatus = EmailStatus.DELIVERED,
                    dateCreated = clock.instant().minus(3, ChronoUnit.DAYS)
            )
            val event2 = createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient3@example.org"),
                    emailStatus = EmailStatus.UNSUBSCRIBE,
                    dateCreated = clock.instant().minus(2, ChronoUnit.DAYS)
            )

            val authentication2 = createAuthentication("example.com")
            createEmailStatusEvent(
                    authentication = authentication2,
                    from = Email("from@example.com"),
                    recipient = Email("recipient@example.com"),
                    emailStatus = EmailStatus.DELIVERED
            )

            val eventsTimeLimit = clock.instant().minus(4, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEvents(
                    accountId = authentication1.account.id,
                    instant = eventsTimeLimit,
                    recipientEmail = Email("recipient3@example.org"),
                    fromEmail = Email("from@example.org"),
                    events = listOf(EmailStatus.DELIVERED, EmailStatus.UNSUBSCRIBE)
            )

            assertEquals(2, emailStatusEvents.size)
            assertEquals(event1.id, emailStatusEvents[0].id)
            assertEquals(event2.id, emailStatusEvents[1].id)
        }

        it("it should not return events outside time limit") {

            val authentication1 = createAuthentication("example.org")
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient1@example.org"),
                    emailStatus = EmailStatus.DELIVERED,
                    dateCreated = clock.instant().minus(3, ChronoUnit.DAYS)

            )
            createEmailStatusEvent(
                    authentication = authentication1,
                    from = Email("from@example.org"),
                    recipient = Email("recipient1@example.org"),
                    emailStatus = EmailStatus.DELIVERED,
                    dateCreated = clock.instant().minus(4, ChronoUnit.DAYS)

            )

            val eventsTimeLimit = clock.instant().minus(2, ChronoUnit.DAYS)

            val emailStatusEvents = emailStatusEventDAO.getEvents(
                    accountId = authentication1.account.id,
                    instant = eventsTimeLimit
            )

            assertEquals(0, emailStatusEvents.size)
        }
    }
})
