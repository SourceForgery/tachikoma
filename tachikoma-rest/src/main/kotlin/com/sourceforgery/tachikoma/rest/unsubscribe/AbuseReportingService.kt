package com.sourceforgery.tachikoma.rest.unsubscribe

import com.linecorp.armeria.common.HttpResponse
import com.linecorp.armeria.common.HttpStatus
import com.linecorp.armeria.common.MediaType
import com.linecorp.armeria.server.annotation.Consumes
import com.linecorp.armeria.server.annotation.ConsumesGroup
import com.linecorp.armeria.server.annotation.Get
import com.linecorp.armeria.server.annotation.Param
import com.linecorp.armeria.server.annotation.Post
import com.linecorp.armeria.server.annotation.Produces
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.database.TransactionManager
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailStatusEventDAO
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.mq.DeliveryNotificationMessage
import com.sourceforgery.tachikoma.mq.IncomingEmailNotificationMessage
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.mq.MessageReportedAbuse
import com.sourceforgery.tachikoma.rest.RestService
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.html.FormMethod.post
import kotlinx.html.body
import kotlinx.html.form
import kotlinx.html.head
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.submitInput
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.textArea
import kotlinx.html.textInput
import kotlinx.html.title
import kotlinx.html.tr
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.Optional
import java.util.Properties

class AbuseReportingService(override val di: DI) : RestService, DIAware {
    private val emailDAO: EmailDAO by instance()
    private val mqSender: MQSender by instance()
    private val emailStatusEventDAO: EmailStatusEventDAO by instance()
    private val transactionManager: TransactionManager by instance()
    private val incomingEmailDAO: IncomingEmailDAO by instance()

    @Get("/abuse/{abuseEmailId}")
    @Produces("text/html; charset=utf-8")
    fun getPage(@Param("abuseEmailId") abuseEmailId: AutoMailId): String {
        return renderPage(
            mailId = abuseEmailId,
            info = null,
            reporterName = null,
            reporterEmail = null,
            error = null,
        )
    }

    @Post("/abuse")
    @ConsumesGroup(
        Consumes("multipart/form-data"),
        Consumes("application/x-www-form-urlencoded")
    )
    suspend fun abuseReport(
        @Param("abuseEmailId") mailId: AutoMailId,
        @Param("info") info: String,
        @Param("reporterName") reporterNameOpt: Optional<String>,
        @Param("reporterEmail") reporterEmailOpt: Optional<Email>,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val reporterName = reporterNameOpt.orElse("Anonymous")
        val reporterEmail = reporterEmailOpt.orElse(null)

        val reportedEmail = emailDAO.getByAutoMailId(mailId)
            ?: return@withContext HttpResponse.of(
                HttpStatus.NOT_FOUND,
                MediaType.PLAIN_TEXT_UTF_8,
                renderPage(
                    mailId,
                    info,
                    reporterNameOpt.orElse(null),
                    reporterEmailOpt.orElse(null),
                    "No email with id $mailId has been sent from this system. Please double-check and send an email to abuse@${mailId.mailDomain} if the data was correct"
                )
            )
        sendAbuseReport(reportedEmail, mailId, reporterName, reporterEmail, info)
        HttpResponse.of(
            HttpStatus.OK,
            MediaType.HTML_UTF_8,
            renderPage(
                mailId,
                null,
                null,
                null,
                "Abuse report sent"
            )
        )
    }

    private suspend fun sendAbuseReport(
        reportedEmail: EmailDBO,
        mailId: AutoMailId,
        reporterName: String?,
        reporterEmail: Email?,
        info: String
    ) {
        val auth = reportedEmail.transaction.authentication
        val fromEmail = NamedEmail(Email(auth.account.mailDomain, "abuse"), "Web abuse report")

        transactionManager.coroutineTx {
            val subject =
                "Abuse report about mail($mailId) to ${reportedEmail.recipient} from $reporterName (${reporterEmail ?: ""})"
            val body =
                """
                        Abuse report from $reporterName (${reporterEmail ?: ""}).
                        Recipient: ${reportedEmail.recipient}
                        Subject: ${reportedEmail.subject}
                        MessageId: ${reportedEmail.messageId}
                        EmailId: ${reportedEmail.id}
                        More info: $info
                """.trimIndent()

            val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()))
            mimeMessage.addHeader("Subject", subject)
            val receiverAddress = InternetAddress(fromEmail.address.address)
            mimeMessage.setFrom(receiverAddress)
            mimeMessage.setRecipient(Message.RecipientType.TO, receiverAddress)
            mimeMessage.setContent(body, MediaType.PLAIN_TEXT_UTF_8.toString())

            @Suppress("BlockingMethodInNonBlockingContext")
            val bytes = mimeMessage.inputStream.use { it.readAllBytes() }
            val incomingEmailDBO = IncomingEmailDBO(
                body = bytes,
                mailFrom = fromEmail.address,
                recipient = fromEmail.address,
                fromEmails = listOf(fromEmail),
                toEmails = listOf(fromEmail),
                replyToEmails = emptyList(),
                account = auth.account,
                subject = subject,
            )
            incomingEmailDAO.save(incomingEmailDBO)
            val notificationMessage = IncomingEmailNotificationMessage.newBuilder()
                .setIncomingEmailMessageId(incomingEmailDBO.id.incomingEmailId)
                .build()

            val emailStatusEvent = EmailStatusEventDBO(
                emailStatus = EmailStatus.SPAM,
                email = reportedEmail,
                metaData = StatusEventMetaData()
            )
            emailStatusEventDAO.save(emailStatusEvent)

            val notificationMessageBuilder = DeliveryNotificationMessage.newBuilder()
                .setCreationTimestamp(emailStatusEvent.dateCreated!!.toTimestamp())
                .setEmailMessageId(reportedEmail.id.emailId)
                .setMessageReportedAbuse(MessageReportedAbuse.getDefaultInstance())

            mqSender.queueIncomingEmailNotification(auth.account.id, notificationMessage)

            mqSender.queueDeliveryNotification(
                reportedEmail.transaction.authentication.account.id,
                notificationMessageBuilder.build()
            )
        }
    }

    internal fun renderPage(
        mailId: AutoMailId?,
        info: String?,
        reporterName: String?,
        reporterEmail: Email?,
        error: String?,
    ): String {
        val htmlDoc = createHTML()
        htmlDoc.head {
            title = "Abuse reporting"
        }
        htmlDoc.body {
            form(method = post) {
                if (error != null) {
                    span {
                        style = "color: red"
                        text(error)
                    }
                }
                table {
                    tr {
                        td {
                            style = "width: 20%"
                            text("Message id on the form of 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX@some-host.com' (without quotation). Looking through the offending email for 'Message-Id: ', remove the <> and put the remainder into this field")
                        }

                        td {
                            textInput {
                                name = "abuseEmailId"
                                if (mailId != null) {
                                    value = mailId.toString()
                                }
                            }
                        }
                    }
                    tr {
                        td {
                            text("Your name. (Optional)")
                        }
                        td {
                            textInput {
                                name = "reporterName"
                                value = reporterName ?: ""
                            }
                        }
                    }
                    tr {
                        td {
                            text("Your email. (Optional)")
                        }
                        td {
                            textInput {
                                name = "reporterEmail"
                                value = reporterEmail?.toString() ?: ""
                            }
                        }
                    }
                    tr {
                        td {
                            text("ALL of the headers in the email and any extra information that may be useful.")
                        }
                        td {
                            textArea(rows = "100", cols = "200") {
                                name = "info"
                                if (info != null) {
                                    text(info)
                                }
                            }
                        }
                    }
                }
                submitInput {
                    name = "submit"
                    value = "Submit abuse complaint"
                }
            }
        }
        return htmlDoc.finalize()
    }

    companion object {
        private val LOGGER = logger()
    }
}