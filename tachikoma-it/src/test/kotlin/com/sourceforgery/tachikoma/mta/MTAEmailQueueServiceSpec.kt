package com.sourceforgery.tachikoma.mta

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.sourceforgery.tachikoma.DatabaseBinder
import com.sourceforgery.tachikoma.TestBinder
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
import com.sourceforgery.tachikoma.hk2.getValue
import com.sourceforgery.tachikoma.hk2.hk2
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.mq.MQSenderMock
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import com.sourceforgery.tachikoma.mq.OutgoingEmailMessage
import com.sourceforgery.tachikoma.mq.QueueMessageWrap
import io.ebean.EbeanServer
import java.time.Clock
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
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
class MTAEmailQueueServiceSpec : Spek({
    lateinit var serviceLocator: ServiceLocator
    val mtaEmailQueueService: MTAEmailQueueService by hk2 { serviceLocator }
    val authentication: AuthenticationMock by hk2 { serviceLocator }
    val ebeanServer: EbeanServer by hk2 { serviceLocator }
    beforeEachTest {
        serviceLocator = ServiceLocatorUtilities.bind(RandomStringUtils.randomAlphanumeric(10), TestBinder(), DatabaseBinder())!!
    }
    afterEachTest { serviceLocator.shutdown() }

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

    describe("MTA queue service test") {
        val mqSequenceFactoryMock: MQSequenceFactoryMock by hk2 { serviceLocator }
        val mqSenderMock: MQSenderMock by hk2 { serviceLocator }
        val clock: Clock by hk2 { serviceLocator }

        lateinit var authenticationDBO: AuthenticationDBO
        lateinit var email: EmailDBO

        beforeEachTest {
            createAuthentication("example.net")
            val databaseConfig: DatabaseConfig by serviceLocator
            val accountDAO: AccountDAO by serviceLocator

            // Setup auth
            val account = accountDAO.getByMailDomain(databaseConfig.mailDomains.first())!!
            authenticationDBO =
                account.authentications
                    .first { it.role == AuthenticationRole.BACKEND }
            authentication.from(authenticationDBO)

            email = EmailDBO(
                recipient = Email("foo@example.net"),
                recipientName = "Nobody",
                messageId = MessageId("sdjklfjklsdfkl@example.net"),
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
            ebeanServer.save(email)
        }

        it("Create email test") {
            val responseObserver = QueueStreamObserver<EmailMessage>()

            mtaEmailQueueService.getEmails(responseObserver, authentication.mailDomain)

            mqSequenceFactoryMock.outgoingEmails.add(QueueMessageWrap(OutgoingEmailMessage.newBuilder()
                .setCreationTimestamp(clock.instant().toTimestamp())
                .setEmailId(email.id.emailId)
                .build()
            ))

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

        it("Receive queue message") {

            val responseObserver = QueueStreamObserver<EmailMessage>()
            val requestStreamObserver = mtaEmailQueueService.getEmails(responseObserver, authentication.mailDomain)

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
        }
    }
})
