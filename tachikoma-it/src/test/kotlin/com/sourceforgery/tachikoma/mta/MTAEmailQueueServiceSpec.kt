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
import com.sourceforgery.tachikoma.grpc.QueueStreamObserver
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import com.sourceforgery.tachikoma.mq.QueueMessageWrap
import com.sourceforgery.tachikoma.testModule
import io.ebean.Database
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

class MTAEmailQueueServiceSpec {
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
        di = DI {
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
        val account = accountDAO.getByMailDomain(databaseConfig.mailDomains.first())!!
        authenticationDBO =
            account.authentications
                .first { it.role == AuthenticationRole.BACKEND }
        authentication.from(authenticationDBO)

        email = EmailDBO(
            recipient = Email("foo@example.net"),
            recipientName = "Nobody",
            messageId = MessageId("sdjklfjklsdfkl@example.com"),
            autoMailId = AutoMailId("sdjklfjklsdfkl@example.net"),
            metaData = emptyMap(),
            transaction = EmailSendTransactionDBO(
                jsonRequest = JsonNodeFactory.instance.objectNode(),
                fromEmail = Email("foodsjklff@example.net"),
                authentication = authenticationDBO,
                metaData = emptyMap(),
                tags = emptyList()
            )
        )
        email.body = "${UUID.randomUUID()}"
        database.save(email)
    }

    fun createAuthentication(domain: String): AuthenticationDBO {
        val database: Database by di.instance()

        val accountDBO = AccountDBO(MailDomain(domain))
        database.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
            login = domain,
            encryptedPassword = UUID.randomUUID().toString(),
            apiToken = UUID.randomUUID().toString(),
            role = AuthenticationRole.BACKEND,
            account = accountDBO
        )
        database.save(authenticationDBO)

        return authenticationDBO
    }

    fun `Create email test`() {
        val responseObserver = QueueStreamObserver<EmailMessage>()

        mtaEmailQueueService.getEmails(responseObserver, authentication.mailDomain)

        mqSequenceFactoryMock.outgoingEmails.add(
            QueueMessageWrap(
                OutgoingEmailMessage.newBuilder()
                    .setCreationTimestamp(clock.instant().toTimestamp())
                    .setEmailId(email.id.emailId)
                    .build()
            )
        )

        mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)
        mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)

        assertEquals(1, responseObserver.queue.size)
        val emailMessage = responseObserver.queue.take().get()
            ?: throw NullPointerException("Should not be a onComplete event")
        assertNotNull(responseObserver)
        assertEquals(emailMessage.body, email.body)
        assertEquals(emailMessage.emailAddress, email.recipient.address)
        assertEquals(emailMessage.emailId, email.id.emailId)
    }

    fun `Receive queue message`() {

        val responseObserver = QueueStreamObserver<EmailMessage>()
        val requestStreamObserver = mtaEmailQueueService.getEmails(responseObserver, authentication.mailDomain)

        requestStreamObserver.onNext(
            MTAQueuedNotification.newBuilder()
                .setEmailId(email.id.emailId)
                .setQueueId("foobarQueueId")
                .setRecipientEmailAddress(email.body!!)
                .setSuccess(true)
                .build()
        )
        mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)
        mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)

        assertEquals(1, mqSenderMock.deliveryNotifications.size)
        database.refresh(email)
        assertEquals("foobarQueueId", email.mtaQueueId)
        assertEquals(1, mqSenderMock.deliveryNotifications.size)
        val notification = mqSenderMock.deliveryNotifications.take()
        assertEquals(email.id.emailId, notification.emailMessageId)
    }
}
