package com.sourceforgery.tachikoma.maildelivery

import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MessageId
import com.sourceforgery.tachikoma.maildelivery.impl.EmailParser.parseBodies
import com.sourceforgery.tachikoma.maildelivery.impl.MailDeliveryService
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.tracking.TrackingDecoderImpl
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoderImpl
import io.mockk.every
import io.mockk.mockk
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Properties
import kotlin.test.assertEquals

class MailDeliveryServiceTest : DIAware {
    val config =
        object : TrackingConfig {
            override val linkSignKey: ByteArray = "poor_key".toByteArray()
            override val baseUrl: URI = URI.create("http://localhost/")
        }

    override val di =
        DI {
            bind<Clock>() with instance(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
            bind<UnsubscribeDecoder>() with singleton { UnsubscribeDecoderImpl(di) }
            bind<TrackingDecoder>() with singleton { TrackingDecoderImpl(di) }
            bind<MailDeliveryService>() with singleton { MailDeliveryService(di) }
            bind<TrackingConfig>() with instance(config)
        }

    private val mailDeliveryService: MailDeliveryService by instance()
    private val unsubscribeDecoder: UnsubscribeDecoder by instance()

    @Test
    fun `replaceLinks test`() {
        val emailDBO =
            mockk<EmailDBO> {
                val emailDBO = this
                every { recipient } returns Email("bar@example.com")
                every { recipientName } returns "Somebody"
                every { emailDBO getProperty "dbId" } returns 123456789L
                every { transaction } returns
                    mockk {
                        every { fromEmail } returns Email("foo@example.com")
                        every { authentication } returns
                            mockk {
                                val authentication = this
                                every { account } returns
                                    mockk {
                                        every { baseUrl } returns URI.create("http://localhost/")
                                    }
                                every { authentication getProperty "dbId" } returns 5678L
                            }
                    }
                every { autoMailId } returns AutoMailId("autoMailId@skldfjkldsf.com")
                every { messageId } returns MessageId("message-id@example.com")
            }
        val wrapAndPackBody =
            mailDeliveryService.wrapAndPackBody(
                (OutgoingEmail.newBuilder()).apply {
                    fromBuilder.apply {
                        email = "foo@example.com"
                        name = "Nobody"
                    }
                    unsubscribeRedirectUri = "http://example.com/foobar/"
                }.build(),
                Instant.now(),
                """
                <a href="http://example.com/1234567890">Link</a>
                <a href="*|UNSUB:http://example.com/barfoo|*">Link</a>
                <a href="*|UNSUB|*">Link</a>
                """.trimIndent(),
                "",
                "",
                emailDBO,
            )
        Email
        val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), wrapAndPackBody.toByteArray().inputStream())
        val html = mimeMessage.parseBodies().htmlBody
        val doc: Document = Jsoup.parse(html)
        val uris =
            doc.select("a[href]")
                .map { it.attr("href") }
        assertEquals(
            listOf(
                "http://localhost/c",
                "http://localhost/unsubscribe",
                "http://localhost/unsubscribe",
            ),
            uris.map {
                it.substringBeforeLast('/')
            },
        )
        assertEquals(
            listOf(
                "http://example.com/1234567890",
                "http://example.com/barfoo",
                "http://example.com/foobar/",
            ),
            uris.map {
                unsubscribeDecoder.decodeUnsubscribeData(it.substringAfterLast('/')).redirectUrl
            },
        )
    }
}
