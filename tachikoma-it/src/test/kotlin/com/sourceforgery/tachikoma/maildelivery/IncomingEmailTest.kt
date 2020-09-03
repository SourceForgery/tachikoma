package com.sourceforgery.tachikoma.maildelivery

import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.GenericDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.maildelivery.impl.IncomingEmailService
import com.sourceforgery.tachikoma.mq.IncomingEmailNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSequenceFactoryMock
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import javax.mail.internet.InternetAddress
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

class IncomingEmailTest : DIAware {
    private val incomingEmailDAO: IncomingEmailDAO = mockk()
    override val di = DI {
        bind<MQSequenceFactoryMock>() with singleton { MQSequenceFactoryMock(di) }
        bind<IncomingEmailService>() with singleton { IncomingEmailService(di) }
        bind<IncomingEmailDAO>() with instance(incomingEmailDAO)
    }
    private val incomingEmailService: IncomingEmailService by instance()
    private val mqSequenceFactoryMock: MQSequenceFactoryMock by instance()

    @Before
    fun b4() {
        clearAllMocks()
    }

    @Test
    fun `parse m1001`() {
        val sample = m1001

        val subject = "Die Hasen und die Frösche (Microsoft Outlook 00)"
        every {
            incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
        } returns IncomingEmailDBO(
            body = sample.envelope,
            account = mockk(),
            mailFrom = from.address,
            recipient = to.address,
            replyToEmails = emptyList(),
            toEmails = emptyList(),
            fromEmails = emptyList(),
            subject = subject
        ).also {
            it.setId(incomingEmailId)
        }

        val mess = processIt(incomingEmailId)
        assertEquals(subject, mess.subject)
        assertEquals(incomingEmailId.incomingEmailId, mess.incomingEmailId.id)
        assertEquals(1, mess.messageAttachmentsCount)
        assertEquals("", mess.messageHtmlBody)
        assertEquals(sample.plainText, mess.messageTextBody.homogenize())
    }

    @Test
    fun `parse m1005`() {
        val subject = "Die Hasen und die Frösche (Netscape Messenger 4.7)"

        val sample = m1005

        every {
            incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
        } returns IncomingEmailDBO(
            body = sample.envelope,
            account = mockk(),
            mailFrom = from.address,
            recipient = to.address,
            replyToEmails = emptyList(),
            toEmails = emptyList(),
            fromEmails = emptyList(),
            subject = subject
        ).also {
            it.setId(incomingEmailId)
        }

        val mess = processIt(incomingEmailId)
        assertEquals(subject, mess.subject)
        assertEquals(incomingEmailId.incomingEmailId, mess.incomingEmailId.id)
        assertEquals(6, mess.messageAttachmentsCount)
        assertEquals(sample.plainText, mess.messageAttachmentsList[0].dataString.homogenize())
        assertEquals(sample.htmlText, mess.messageAttachmentsList[1].dataString.homogenize())
        assertEquals(sample.htmlText, mess.messageHtmlBody.homogenize())
        assertEquals(sample.plainText, mess.messageTextBody.homogenize())
    }

    @Test
    fun `parse m1006`() {
        val subject = "Die Hasen und die Frösche (Netscape Messenger 4.7)"

        val sample = m1006

        every {
            incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
        } returns IncomingEmailDBO(
            body = sample.envelope,
            account = mockk(),
            mailFrom = from.address,
            recipient = to.address,
            replyToEmails = emptyList(),
            toEmails = emptyList(),
            fromEmails = emptyList(),
            subject = subject
        ).also {
            it.setId(incomingEmailId)
        }

        val mess = processIt(incomingEmailId)
        assertEquals(subject, mess.subject)
        assertEquals(incomingEmailId.incomingEmailId, mess.incomingEmailId.id)
        assertEquals(5, mess.messageAttachmentsCount)
        assertEquals(sample.htmlText, mess.messageAttachmentsList[0].dataString.homogenize())
        assertEquals(sample.htmlText, mess.messageHtmlBody.homogenize())
        assertEquals(sample.plainText, mess.messageTextBody.homogenize())
    }

    @Test
    fun `parse m2008`() {
        val subject = "Die Hasen und die Frösche (Netscape Messenger 4.7)"

        val sample = m2008

        every {
            incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
        } returns IncomingEmailDBO(
            body = sample.envelope,
            account = mockk(),
            mailFrom = from.address,
            recipient = to.address,
            replyToEmails = emptyList(),
            toEmails = emptyList(),
            fromEmails = emptyList(),
            subject = subject
        ).also {
            it.setId(incomingEmailId)
        }

        val mess = processIt(incomingEmailId)
        assertEquals(subject, mess.subject)
        assertEquals(incomingEmailId.incomingEmailId, mess.incomingEmailId.id)
        assertEquals(5, mess.messageAttachmentsCount)
        assertEquals(sample.plainText, mess.messageAttachmentsList[0].dataString.homogenize())
        assertEquals(sample.htmlText, mess.messageAttachmentsList[1].dataString.homogenize())
        assertEquals(sample.htmlText, mess.messageHtmlBody.homogenize())
        assertEquals(sample.plainText, mess.messageTextBody.homogenize())
    }

    fun assertEmail(first: NamedEmail, second: NamedEmailAddress) {
        assertEquals(first.address.address, second.email)
        assertEquals(first.name, second.name)
    }

    fun processIt(incomingEmailId: IncomingEmailId): IncomingEmail {
        return runBlocking {
            mqSequenceFactoryMock.incomingEmails.send(
                IncomingEmailNotificationMessage.newBuilder()
                    .setIncomingEmailMessageId(incomingEmailId.incomingEmailId)
                    .build()
            )
            withTimeout(100000) {
                incomingEmailService.streamIncomingEmails(
                    parameters = INCLUDE_ALL,
                    accountId = accountId,
                    mailDomain = mailDomain,
                    authenticationId = authenticationId
                ).first()
            }
        }
    }

    companion object {
        fun parseEmail(email: String): NamedEmail =
            InternetAddress.parse(email)
                .first()
                .let { NamedEmail(it.address, it.personal) }

        val INCLUDE_ALL = IncomingEmailParameters
            .newBuilder()
            .setIncludeMessageAttachments(true)
            .setIncludeMessageHeader(true)
            .setIncludeMessageParsedBodies(true)
            .setIncludeMessageWholeEnvelope(true)
            .build()

        private val from = parseEmail(""""Doug Sauder" <doug@example.com>""")
        private val to = parseEmail(""""Jürgen Schmürgen" <schmuergen@example.com>""")
        private val mailDomain = MailDomain("example.com")
        private val incomingEmailId = IncomingEmailId(667)
        private val authenticationId = AuthenticationId(1)
        private val accountId = AccountId(1)
    }
}

private fun IncomingEmailDBO.setId(incomingEmailId: IncomingEmailId) {
    val field = GenericDBO::class.java.getDeclaredField("dbId")
    field.isAccessible = true
    field.set(this, incomingEmailId.incomingEmailId)
}
