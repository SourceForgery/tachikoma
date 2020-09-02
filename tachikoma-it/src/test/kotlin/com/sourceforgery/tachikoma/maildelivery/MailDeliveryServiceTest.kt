package com.sourceforgery.tachikoma.maildelivery

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.DAOHelper
import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.QueueStreamObserver
import com.sourceforgery.tachikoma.grpc.frontend.Attachment
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.RelatedAttachment
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.testModule
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64
import javax.mail.internet.cleanUniqueValueMock
import javax.mail.internet.mockUniqueValue
import org.jsoup.Jsoup
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

class MailDeliveryServiceTest : DIAware {
    override val di = DI {
        importOnce(testModule(), allowOverride = true)
        bind<JobMessageFactory>() with singleton { JobMessageFactory(di) }
        bind<MailDeliveryService>() with singleton { MailDeliveryService(di) }
    }

    val mailDeliveryService: MailDeliveryService by instance()
    val daoHelper: DAOHelper by instance()
    val emailDAO: EmailDAO by instance()
    private val clocker: Clocker by instance()

    @Rule
    @JvmField
    val testName = TestName()

    @Before
    fun b4() {
        mockUniqueValue("XXXXXX")
        clocker.clock = Clock.fixed(Instant.parse("2007-12-03T10:15:30.00Z"), ZoneId.of("UTC"))
    }

    @After
    fun aft() {
        cleanUniqueValueMock()
    }

    @Test
    fun `Simple email inlined css`() {
        val input = this.javaClass.getResource("/wrapAndPackBody/parseHTML/simple/input.html").readText()
        val expected = Jsoup.parse(this.javaClass.getResource("/wrapAndPackBody/parseHTML/simple/expected.html").readText()).html()
        val actual = mailDeliveryService.parseHTML(input, "", true).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Complex email inlined css`() {
        val input = this.javaClass.getResource("/wrapAndPackBody/parseHTML/complex/input.html").readText()
        val expected = Jsoup.parse(this.javaClass.getResource("/wrapAndPackBody/parseHTML/complex/expected.html").readText()).html()
        val actual = Jsoup.parse(mailDeliveryService.parseHTML(input, "", true).html()).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Simple email no css inlineing`() {
        val input = this.javaClass.getResource("/wrapAndPackBody/parseHTML/simple/input.html").readText()
        val expected = Jsoup.parse(this.javaClass.getResource("/wrapAndPackBody/parseHTML/simple/expectedNoInlining.html").readText()).html()
        val actual = mailDeliveryService.parseHTML(input, "", false).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Complex email no css inlining`() {
        val input = this.javaClass.getResource("/wrapAndPackBody/parseHTML/complex/input.html").readText()
        val expected = Jsoup.parse(this.javaClass.getResource("/wrapAndPackBody/parseHTML/complex/expectedNoInlining.html").readText()).html()
        val actual = Jsoup.parse(mailDeliveryService.parseHTML(input, "", false).html()).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Send emails with attachment`() {
        val authentication = daoHelper.createAuthentication(fromEmail.toNamedEmail().address.domain)
        val email = OutgoingEmail.newBuilder()
            .addRecipients(EmailRecipient.newBuilder().setNamedEmail(validEmail))
            .setTimeZone("America/New_York")
            .addAttachments(
                Attachment.newBuilder()
                    .setContentType("application/pdf")
                    .setData(ByteString.copyFrom(data))
                    .setFileName("NotReally.pdf")
            )
            .addRelatedAttachments(
                RelatedAttachment.newBuilder()
                    .setContentId("68b12347-e804-48f8-a9d4-86a1d1acfda3")
                    .setFileName("transparent.gif")
                    .setContentType("image/gif")
                    .setData(ByteString.copyFrom(pixel))
            )
            .setFrom(fromEmail)
            .setStatic(
                StaticBody.newBuilder().setPlaintextBody(
                    """This is a test
                            |                                      .
                            |.
                            |}
                            |.                 ${""}
                            |"""
                        .trimMargin()
                )
                    .setHtmlBody("""
                        <h1>This is a test</h1>
                        <img src="cid:68b12347-e804-48f8-a9d4-86a1d1acfda3">
                    """.trimIndent())

                    .setSubject("Test mail subject")
            )
            .build()
        val responseObserver = QueueStreamObserver<EmailQueueStatus>()
        mailDeliveryService.sendEmail(
            request = email,
            responseObserver = responseObserver,
            authenticationId = authentication.id
        )
        val queued = responseObserver.take(500)
        val byEmailId = emailDAO.getByEmailId(queued.emailId.toEmailId())!!

        val expected = this.javaClass.getResourceAsStream("/attachment_email.txt").use {
            it.readBytes().toString(StandardCharsets.UTF_8)
        }
        assertEquals(expected, byEmailId.body!!)
    }

    companion object {
        val validEmail = NamedEmailAddress.newBuilder().setEmail("foo@example.com").setName("Valid Email").build()
        val fromEmail = NamedEmailAddress.newBuilder().setEmail("from@example.com").setName("Valid From Email").build()

        val data = Base64.getDecoder().decode("dt6J5W7J+3hrduLSGtgij5IQrnc=")!!
        val pixel = Base64.getDecoder().decode("R0lGODlhAQABAAAAACH5BAEAAAAALAAAAAABAAEAAAI=")
    }
}
