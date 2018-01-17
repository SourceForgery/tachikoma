package com.sourceforgery.tachikoma.database.dao

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.Hk2TestBinder
import com.sourceforgery.tachikoma.common.BlockedReason
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
import org.glassfish.hk2.utilities.ServiceLocatorUtilities
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(JUnitPlatform::class)
internal class BlockedEmailDAOSpec : Spek({
    val serviceLocator = ServiceLocatorUtilities.bind(Hk2TestBinder())
    val blockedEmailDAO = serviceLocator.getService(BlockedEmailDAO::class.java)
    val dbObjectMapper = serviceLocator.getService(DBObjectMapper::class.java)

    val PRINTER = JsonFormat.printer()!!

    fun getEmailStatusEvent(from: Email, recipient: Email, emailStatus: EmailStatus): EmailStatusEventDBO {
        val account = AccountDBO(MailDomain("example.com"))
        val authentication = AuthenticationDBO(
                encryptedPassword = null,
                apiToken = null,
                backend = true,
                account = account
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

    describe("BlockedEmailDAO") {

        afterEachTest {
            // Clear H2 DB
        }

        it("should return null if e-mail is not blocked") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val blockedReason = blockedEmailDAO.getBlockedReason(from, recipient)

            assertEquals(null, blockedReason)
        }

        it("should return blocked reason if e-mail is blocked") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val emailStatusEvent = getEmailStatusEvent(from, recipient, EmailStatus.UNSUBSCRIBE)

            blockedEmailDAO.block(emailStatusEvent)

            val blockedReason = blockedEmailDAO.getBlockedReason(from, recipient)

            assertEquals(BlockedReason.UNSUBSCRIBED, blockedReason)

            // TODO Make sure DB is cleared
            blockedEmailDAO.unblock(emailStatusEvent)
        }

        it("should should be possible to unblock a blocked e-mail") {

            val from = Email("from@example.com")
            val recipient = Email("recipient@example.com")

            val emailStatusEvent = getEmailStatusEvent(from, recipient, EmailStatus.HARD_BOUNCED)

            blockedEmailDAO.block(emailStatusEvent)

            assertEquals(BlockedReason.HARD_BOUNCED, blockedEmailDAO.getBlockedReason(from, recipient))

            blockedEmailDAO.unblock(emailStatusEvent)

            assertEquals(null, blockedEmailDAO.getBlockedReason(from, recipient))
        }
    }
})