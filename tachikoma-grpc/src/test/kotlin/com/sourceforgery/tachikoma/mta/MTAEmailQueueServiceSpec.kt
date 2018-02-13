package com.sourceforgery.tachikoma.mta

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.auth.AuthenticationMock
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.config.DatabaseConfig
import com.sourceforgery.tachikoma.database.dao.AccountDAO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.NullStreamObserver
import com.sourceforgery.tachikoma.grpc.QueueStreamObserver
import com.sourceforgery.tachikoma.hk2.get
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import com.sourceforgery.tachikoma.mq.QueueMessageWrap
import io.ebean.EbeanServer
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(JUnitPlatform::class)
class MTAEmailQueueServiceSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    lateinit var mtaEmailQueueService: MTAEmailQueueService
    lateinit var authentication: AuthenticationMock
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder(), DatabaseBinder())!!
        mtaEmailQueueService = serviceLocator.get()
        authentication = serviceLocator.get()
    }
    afterEachTest { serviceLocator.shutdown() }

    describe("MTA queue auth", {
        it("with invalid auth", {
            authentication.invalidAuth()
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })

        it("with frontend auth", {
            authentication.from(AuthenticationRole.FRONTEND, MailDomain("example.com"))
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })

        it("with frontendadmin auth", {
            authentication.from(AuthenticationRole.FRONTEND_ADMIN, MailDomain("example.com"))
            assertFailsWith(InvalidOrInsufficientCredentialsException::class, {
                mtaEmailQueueService.getEmails(NullStreamObserver())
            })
        })
    })

    describe("MTA queue service test", {
        lateinit var mqSequenceFactoryMock: MQSequenceFactoryMock
        lateinit var mqSenderMock: MQSenderMock
        lateinit var ebeanServer: EbeanServer
        lateinit var clock: Clock

        lateinit var authenticationDBO: AuthenticationDBO
        lateinit var email: EmailDBO

        beforeEachTest {
            ebeanServer = serviceLocator.get()
            mqSequenceFactoryMock = serviceLocator.get()
            clock = serviceLocator.get()
            mqSenderMock = serviceLocator.get()
            val databaseConfig: DatabaseConfig = serviceLocator.get()
            val accountDAO: AccountDAO = serviceLocator.get()

            // Setup auth
            val account = accountDAO.getByMailDomain(databaseConfig.mailDomain)!!
            authenticationDBO =
                    account.authentications
                            .first { it.role == AuthenticationRole.BACKEND }
            authentication.from(authenticationDBO)

            email = EmailDBO(
                    recipient = Email("foo@example.net"),
                    recipientName = "Nobody",
                    messageId = MessageId("sdjklfjklsdfkl@example.net"),
                    metaData = HashMap(),
                    transaction = EmailSendTransactionDBO(
                            jsonRequest = JsonNodeFactory.instance.objectNode(),
                            fromEmail = Email("foodsjklff@example.net"),
                            authentication = authenticationDBO,
                            metaData = HashMap(),
                            tags = emptyList()
                    )
            )
            email.body = "${UUID.randomUUID()}"
            ebeanServer.save(email)
        }

        it("Create email test", {
            val responseObserver = QueueStreamObserver<EmailMessage>()

            mtaEmailQueueService.getEmails(responseObserver)

            mqSequenceFactoryMock.outgoingEmails.add(QueueMessageWrap(OutgoingEmailMessage.newBuilder()
                    .setCreationTimestamp(clock.instant().toTimestamp())
                    .setEmailId(email.id.emailId)
                    .build()
            ))

            mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)
            mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)

            assertEquals(responseObserver.queue.size, 1)
            val emailMessage = responseObserver.queue.take().get()
                    ?: throw NullPointerException("Should not be a onComplete event")
            assertNotNull(responseObserver)
            assertEquals(emailMessage.body, email.body)
            assertEquals(emailMessage.emailAddress, email.recipient.address)
            assertEquals(emailMessage.emailId, email.id.emailId)
        })

        it("Receive queue message", {

            val responseObserver = QueueStreamObserver<EmailMessage>()
            val requestStreamObserver = mtaEmailQueueService.getEmails(responseObserver)

            requestStreamObserver.onNext(MTAQueuedNotification.newBuilder()
                    .setEmailId(email.id.emailId)
                    .setQueueId("foobarQueueId")
                    .setRecipientEmailAddress(email.body!!)
                    .setSuccess(true)
                    .build()
            )
            mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)
            mqSequenceFactoryMock.outgoingEmails.offer(QueueMessageWrap(null), 1, TimeUnit.SECONDS)

            assertEquals(1, mqSenderMock.deliveryNotifications.size)
            ebeanServer.refresh(email)
            assertEquals("foobarQueueId", email.mtaQueueId)
            assertEquals(1, mqSenderMock.deliveryNotifications.size)
            val notification = mqSenderMock.deliveryNotifications.take()
            assertEquals(email.id.emailId, notification.emailMessageId)
        })
    })
})