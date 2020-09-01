package com.sourceforgery.tachikoma.maildelivery

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.DAOHelper
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.QueueStreamObserver
import com.sourceforgery.tachikoma.grpc.frontend.Attachment
import com.sourceforgery.tachikoma.grpc.frontend.NamedEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.StaticBody
import com.sourceforgery.tachikoma.grpc.frontend.toEmailId
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.testModule
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
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
            .addAttachments(
                Attachment.newBuilder()
                    .setContentType("application/pdf")
                    .setData(ByteString.copyFrom(data))
                    .setFileName("NotReally.pdf")
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
                ).setSubject("Test mail subject")
            )
            .build()
        val responseObserver = QueueStreamObserver<EmailQueueStatus>()
        runBlocking {
            withTimeout(10000) {
                mailDeliveryService.sendEmail(
                    request = email,
                    authenticationId = authentication.id
                ).first()
            }
        }
        val queued = responseObserver.take(500)
        val byEmailId = emailDAO.getByEmailId(queued.emailId.toEmailId())!!
        val boundary = Regex("\tboundary=\"(.*?)\"").find(byEmailId.body!!)!!.groupValues[1]

        val modifiedBody = byEmailId.body!!.replace(boundary, "XXXXXX")
            .replace(Regex("Date: .*"), "Date: XXXXX")

        val expected = this.javaClass.getResourceAsStream("/attachment_email.txt").use {
            it.readBytes().toString(StandardCharsets.UTF_8)
        }
        assertEquals(expected, modifiedBody)
    }

    companion object {
        val validEmail = NamedEmailAddress.newBuilder().setEmail("foo@example.com").setName("Valid Email").build()
        val fromEmail = NamedEmailAddress.newBuilder().setEmail("from@example.com").setName("Valid From Email").build()

        val data = Base64.getDecoder().decode("dt6J5W7J+3hrduLSGtgij5IQrnc=")!!
    }
}
