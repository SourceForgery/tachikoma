package com.sourceforgery.tachikoma.mta

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.sourceforgery.tachikoma.auth.AuthenticationMock
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MTAEmailQueueServiceTest {
    lateinit var di: DI
    lateinit var authenticationDBO: AuthenticationDBO
    lateinit var email: EmailDBO
    lateinit var authentication: AuthenticationMock
    lateinit var mqSequenceFactoryMock: MQSequenceFactoryMock
    lateinit var mqSenderMock: MQSenderMock
    lateinit var clock: Clock
    lateinit var mtaEmailQueueService: MTAEmailQueueService
    lateinit var database: Database

    @Before
    fun `create di`() {
        di =
            DI {
                import(testModule())
            }
        authentication = di.direct.instance()
        mqSequenceFactoryMock = di.direct.instance()
        mqSenderMock = di.direct.instance()
        clock = di.direct.instance()
        mtaEmailQueueService = di.direct.instance()
        database = di.direct.instance()
    }

    @Before
    fun `create prerequisites`() {
        createAuthentication("example.net")
        val databaseConfig: DatabaseConfig by di.instance()
        val accountDAO: AccountDAO by di.instance()

        // Setup auth
        val account = accountDAO.get(databaseConfig.mailDomains.first())!!
        authenticationDBO =
            account.authentications
                .first { it.role == AuthenticationRole.BACKEND }
        authentication.from(authenticationDBO)

        email =
            EmailDBO(
                recipient = Email("foo@example.net"),
                recipientName = "Nobody",
                messageId = MessageId("sdjklfjklsdfkl@example.com"),
                autoMailId = AutoMailId("sdjklfjklsdfkl@example.net"),
                metaData = emptyMap(),
                transaction =
                    EmailSendTransactionDBO(
                        jsonRequest = JsonNodeFactory.instance.objectNode(),
                        fromEmail = Email("foodsjklff@example.net"),
                        authentication = authenticationDBO,
                        metaData = emptyMap(),
                        tags = emptySet(),
                    ),
            )
        email.body = "${UUID.randomUUID()}"
        database.save(email)
    }

    fun createAuthentication(domain: String): AuthenticationDBO {
        val database: Database by di.instance()

        val accountDBO = AccountDBO(MailDomain(domain))
        database.save(accountDBO)

        val authenticationDBO =
            AuthenticationDBO(
                login = domain,
                encryptedPassword = UUID.randomUUID().toString(),
                apiToken = UUID.randomUUID().toString(),
                role = AuthenticationRole.BACKEND,
                account = accountDBO,
            )
        database.save(authenticationDBO)

        return authenticationDBO
    }

    fun `Create email test`() =
        runBlocking {
            val requests = Channel<MTAQueuedNotification>()

            val notifications = mtaEmailQueueService.getEmails(requests.consumeAsFlow(), authentication.mailDomain)

            mqSequenceFactoryMock.outgoingEmails.send(
                OutgoingEmailMessage.newBuilder()
                    .setCreationTimestamp(clock.instant().toTimestamp())
                    .setEmailId(email.id.emailId)
                    .build(),
            )

            val emailMessage =
                runBlocking {
                    withTimeout(1000L) {
                        notifications.take(1)
                            .firstOrNull()
                    }
                }
            requests.close()
            assertNotNull(emailMessage)
            assertEquals(emailMessage.body, email.body)
            assertEquals(emailMessage.emailAddress, email.recipient.address)
            assertEquals(emailMessage.emailId, email.id.emailId)
        }

    fun `Receive queue message`() =
        runBlocking {
            val requests = Channel<MTAQueuedNotification>()
            mtaEmailQueueService.getEmails(requests.consumeAsFlow(), authentication.mailDomain)

            requests.send(
                MTAQueuedNotification.newBuilder()
                    .setEmailId(email.id.emailId)
                    .setQueueId("foobarQueueId")
                    .setRecipientEmailAddress(email.body!!)
                    .setSuccess(true)
                    .build(),
            )

            requests.close()

            assertEquals(1, mqSenderMock.deliveryNotifications.size)
            database.refresh(email)
            assertEquals("foobarQueueId", email.mtaQueueId)
            assertEquals(1, mqSenderMock.deliveryNotifications.size)
            val notification = mqSenderMock.deliveryNotifications.take()
            assertEquals(email.id.emailId, notification.deliveryNotification.emailMessageId)
        }
}
