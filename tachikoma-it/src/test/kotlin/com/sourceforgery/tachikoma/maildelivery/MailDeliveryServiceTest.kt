package com.sourceforgery.tachikoma.maildelivery

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.DAOHelper
import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.Attachment
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.RelatedAttachment
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.testModule
import jakarta.mail.internet.cleanUniqueValueMock
import jakarta.mail.internet.mockUniqueValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Base64

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
        val input = parseJsoupResource("/wrapAndPackBody/parseHTML/simple/input.html").html()
        val expected = parseJsoupResource("/wrapAndPackBody/parseHTML/simple/expected.html").html()
        val actual = mailDeliveryService.parseHTML(input, "", true).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Complex email inlined css`() {
        val input = parseJsoupResource("/wrapAndPackBody/parseHTML/complex/input.html").html()
        val expected = parseJsoupResource("/wrapAndPackBody/parseHTML/complex/expected.html").html()
        val actual = parseJsoupHtml(mailDeliveryService.parseHTML(input, "", true).html()).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Simple email no css inlineing`() {
        val input = parseJsoupResource("/wrapAndPackBody/parseHTML/simple/input.html").html()
        val expected = parseJsoupResource("/wrapAndPackBody/parseHTML/simple/expectedNoInlining.html").html()
        val actual = mailDeliveryService.parseHTML(input, "", false).html()
        assertEquals(expected, actual)
    }

    @Test
    fun `Complex email no css inlining`() {
        val input = parseJsoupResource("/wrapAndPackBody/parseHTML/complex/input.html").html()
        val expected = parseJsoupResource("/wrapAndPackBody/parseHTML/complex/expectedNoInlining.html").html()
        val actual = parseJsoupHtml(mailDeliveryService.parseHTML(input, "", false).html()).html()
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
                            |
                    """
                        .trimMargin()
                )
                    .setHtmlBody(
                        """
                        <h1>This is a test</h1>
                        <img src="cid:68b12347-e804-48f8-a9d4-86a1d1acfda3">
                        """.trimIndent()
                    )
                    .setSubject("Test mail subject")
            )
            .build()
        val queued = runBlocking {
            withTimeout(10000) {
                mailDeliveryService.sendEmail(
                    request = email,
                    authenticationId = authentication.id
                ).first()
            }
        }
        val byEmailId = emailDAO.getByEmailId(queued.emailId.toEmailId())!!

        val expected = this.javaClass.getResourceAsStream("/attachment_email.txt")!!.use {
            it.readBytes().toString(StandardCharsets.UTF_8)
        }
        assertEquals(expected, byEmailId.body!!)
    }

    private fun parseJsoupHtml(html: String): Document =
        Jsoup.parse(html)

    private fun parseJsoupResource(resourceName: String): Document =
        Jsoup.parse(
            requireNotNull(javaClass.getResource(resourceName)) {
                "Didn't find $resourceName"
            }.readText()
        )

    companion object {
        val validEmail: NamedEmailAddress = NamedEmailAddress.newBuilder().setEmail("foo@example.com").setName("Valid Email").build()
        val fromEmail: NamedEmailAddress = NamedEmailAddress.newBuilder().setEmail("from@example.com").setName("Valid From Email").build()

        val data = Base64.getDecoder().decode("dt6J5W7J+3hrduLSGtgij5IQrnc=")!!
        val pixel = Base64.getDecoder().decode("R0lGODlhAQABAAAAACH5BAEAAAAALAAAAAABAAEAAAI=")!!
    }
}
