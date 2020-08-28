package com.sourceforgery.tachikoma.maildelivery

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.GenericDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.grpc.QueueStreamObserver
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
import com.sourceforgery.tachikoma.mq.QueueMessageWrap
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.nio.charset.Charset
import javax.mail.internet.InternetAddress
import kotlin.test.assertEquals

class IncomingEmailTest : DIAware {
    private val incomingEmailDAO: IncomingEmailDAO = mockk()
    override val di = DI {
        bind<MQSequenceFactoryMock>() with singleton { MQSequenceFactoryMock(di) }
        bind<IncomingEmailService>() with singleton { IncomingEmailService(di) }
        bind<IncomingEmailDAO>() with instance(incomingEmailDAO)
    }
    private val incomingEmailService: IncomingEmailService by instance()
    private val mqSequenceFactoryMock: MQSequenceFactoryMock by instance()
    private val observer = QueueStreamObserver<IncomingEmail>()

    @Before
    fun b4() {
        clearAllMocks()
    }

    @Test
    fun `stream incoming emails`() {

        incomingEmailService.streamIncomingEmails(
            responseObserver = observer,
            parameters = IncomingEmailParameters
                .newBuilder()
                .setIncludeMessageAttachments(true)
                .setIncludeMessageHeader(true)
                .setIncludeMessageParsedBodies(true)
                .setIncludeMessageWholeEnvelope(true)
                .build(),
            accountId = accountId,
            mailDomain = mailDomain,
            authenticationId = authenticationId
        )

        val from = parseEmail(""""Doug Sauder" <doug@example.com>""")
        val to = parseEmail(""""Jürgen Schmürgen" <schmuergen@example.com>""")
        val subject = "Die Hasen und die Frösche (Microsoft Outlook 00)"
        every {
            incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
        } returns IncomingEmailDBO(
            body = """
                |From: "Doug Sauder" <doug@example.com>
                |To: "Jürgen Schmürgen" <schmuergen@example.com>
                |Subject: Die Hasen und die Frösche (Microsoft Outlook 00)
                |Date: Wed, 17 May 2000 19:08:29 -0400
                |Message-ID: <NDBBIAKOPKHFGPLCODIGIEKBCHAA.doug@example.com>
                |MIME-Version: 1.0
                |Content-Type: text/plain;
                |	charset="iso-8859-1"
                |Content-Transfer-Encoding: 8bit
                |X-Priority: 3 (Normal)
                |X-MSMail-Priority: Normal
                |X-Mailer: Microsoft Outlook IMO, Build 9.0.2416 (9.0.2910.0)
                |Importance: Normal
                |X-MimeOLE: Produced By Microsoft MimeOLE V5.00.2314.1300
                |
                |Die Hasen und die Frösche
                |
                |Die Hasen klagten einst über ihre mißliche Lage; "wir leben", sprach ein
                |Redner, "in steter Furcht vor Menschen und Tieren, eine Beute der Hunde, der
                |Adler, ja fast aller Raubtiere! Unsere stete Angst ist ärger als der Tod
                |selbst. Auf, laßt uns ein für allemal sterben."
                |
                |In einem nahen Teich wollten sie sich nun ersäufen; sie eilten ihm zu;
                |allein das außerordentliche Getöse und ihre wunderbare Gestalt erschreckte
                |eine Menge Frösche, die am Ufer saßen, so sehr, daß sie aufs schnellste
                |untertauchten.
                |
                |"Halt", rief nun eben dieser Sprecher, "wir wollen das Ersäufen noch ein
                |wenig aufschieben, denn auch uns fürchten, wie ihr seht, einige Tiere,
                |welche also wohl noch unglücklicher sein müssen als wir."
            """.trimMargin().toByteArray(Charset.forName("ISO-8859-15")),
            account = mockk(),
            fromEmail = from.address,
            fromName = from.name,
            receiverEmail = to.address,
            receiverName = to.name,
            subject = subject
        ) .also {
            it.setId(incomingEmailId)
        }



        val mess = processIt(incomingEmailId)
        assertEquals(subject, mess.subject)
        assertEmail(to, mess.to)
        assertEmail(from, mess.from)
        assertEquals(incomingEmailId.incomingEmailId, mess.incomingEmailId.id)
        assertEquals(1, mess.messageAttachmentsCount)
        assertEquals("", mess.messageHtmlBody)
        assertEquals("", mess.messageTextBody)
    }

    fun assertEmail(first: NamedEmail, second: NamedEmailAddress) {
        assertEquals(first.address.address, second.email)
        assertEquals(first.name, second.name)
    }

    fun processIt(incomingEmailId: IncomingEmailId): IncomingEmail {
        mqSequenceFactoryMock.incomingEmails.add(
            QueueMessageWrap(
                IncomingEmailNotificationMessage.newBuilder()
                    .setIncomingEmailMessageId(incomingEmailId.incomingEmailId)
                    .build()
            )
        )
        return observer.take(10000)
    }



    companion object {

        fun parseEmail(email: String): NamedEmail =
            InternetAddress.parse(email)
                .first()
                .let { NamedEmail(it.address, it.personal) }

        private val mailDomain = MailDomain("example.com")
        private val from = Email("doug@example.com")
        private val to = NamedEmail("schmuergen@example.com", "Jürgen Schmürgen")
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
