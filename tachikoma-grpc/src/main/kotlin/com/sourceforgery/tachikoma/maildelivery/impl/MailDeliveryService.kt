package com.sourceforgery.tachikoma.maildelivery.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.Struct
import com.google.protobuf.util.JsonFormat
import com.samskivert.mustache.BasicCollector
import com.samskivert.mustache.Mustache
import com.sourceforgery.jersey.uribuilder.JerseyUriBuilder
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.database.TransactionManager
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailDAO
import com.sourceforgery.tachikoma.database.dao.EmailSendTransactionDAO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.database.objects.recipientNamedEmail
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.exceptions.InvalidOrInsufficientCredentialsException
import com.sourceforgery.tachikoma.grpc.frontend.emptyToNull
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Queued
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Rejected
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcRejectReason
import com.sourceforgery.tachikoma.grpc.frontend.toNamedEmail
import com.sourceforgery.tachikoma.grpc.frontend.tracking.UrlTrackingData
import com.sourceforgery.tachikoma.grpc.frontend.unsubscribe.UnsubscribeData
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MessageIdFactory
import com.sourceforgery.tachikoma.maildelivery.getPlainText
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import com.sourceforgery.tachikoma.mq.MQSender
import com.sourceforgery.tachikoma.tracking.TrackingDecoder
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeConfig
import com.sourceforgery.tachikoma.unsubscribe.UnsubscribeDecoder
import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.activation.DataHandler
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.annotations.TestOnly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.StringTokenizer
import java.util.concurrent.ConcurrentHashMap

@Suppress("EXPERIMENTAL_API_USAGE")
class MailDeliveryService(override val di: DI) : DIAware {
    private val trackingConfig: TrackingConfig by instance()
    private val unsubscribeConfig: UnsubscribeConfig by instance()
    private val dbObjectMapper: DBObjectMapper by instance()
    private val emailDAO: EmailDAO by instance()
    private val emailSendTransactionDAO: EmailSendTransactionDAO by instance()
    private val blockedEmailDAO: BlockedEmailDAO by instance()
    private val mqSender: MQSender by instance()
    private val jobMessageFactory: JobMessageFactory by instance()
    private val transactionManager: TransactionManager by instance()
    private val trackingDecoderImpl: TrackingDecoder by instance()
    private val unsubscribeDecoderImpl: UnsubscribeDecoder by instance()
    private val authenticationDAO: AuthenticationDAO by instance()
    private val messageIdFactory: MessageIdFactory by instance()
    private val clock: Clock by instance()

    private val mustacheCompiler: Mustache.Compiler =
        Mustache.compiler()
            .withCollector(
                object : BasicCollector() {
                    override fun <K : Any?, V : Any?> createFetcherCache(): MutableMap<K, V> {
                        return ConcurrentHashMap()
                    }
                }
            )

    private fun baseUrl(emailDBO: EmailDBO): URI =
        emailDBO.transaction.authentication.account.baseUrl ?: trackingConfig.baseUrl

    suspend fun sendEmail(
        request: OutgoingEmail,
        authenticationId: AuthenticationId
    ): Flow<EmailQueueStatus> = channelFlow {
        val auth = authenticationDAO.getActiveById(authenticationId)!!
        val fromEmail = request.from.toNamedEmail().address
        if (fromEmail.domain != auth.account.mailDomain) {
            throw InvalidOrInsufficientCredentialsException("${auth.account.mailDomain} is not allowed to send emails with from domain: ${fromEmail.domain}")
        }

        val transaction = EmailSendTransactionDBO(
            jsonRequest = getRequestData(request),
            fromEmail = fromEmail,
            authentication = auth,
            bcc = request.bccList.map { it.toEmail().address },
            metaData = request.trackingData.metadataMap,
            tags = request.trackingData.tagsList
        )
        val requestedSendTime =
            if (request.hasSendAt()) {
                request.sendAt.toInstant()
            } else {
                Instant.EPOCH
            }

        transactionManager.coroutineTx {
            emailSendTransactionDAO.save(transaction)

            for (recipient in request.recipientsList) {

                val recipientEmail = auth.recipientOverride
                    ?.let {
                        NamedEmail(it, "Overridden email")
                    }
                    ?: recipient.toNamedEmail()

                val blockedReason = blockedEmailDAO.getBlockedReason(
                    accountDBO = auth.account,
                    recipient = recipientEmail.address,
                    from = fromEmail
                )

                if (blockedReason != null) {
                    send(
                        EmailQueueStatus.newBuilder()
                            .setRejected(
                                Rejected.newBuilder()
                                    .setRejectReason(blockedReason.toGrpcRejectReason())
                                    .build()
                            )
                            .setTransactionId(transaction.id.toGrpcInternal())
                            .setRecipient(recipientEmail.address.toGrpcInternal())
                            .build()
                    )
                    LOGGER.debug { "Email to ${recipientEmail.address} had unsubscribed emails from $fromEmail, hence bounced" }
                    // Blocked
                    continue
                }

                val messageId = messageIdFactory.createMessageId(
                    domain = fromEmail.domain
                )
                val unsubscribeDomainOverride = unsubscribeConfig.unsubscribeDomainOverride
                val autoMailId = if (unsubscribeDomainOverride == null) {
                    AutoMailId("$messageId")
                } else {
                    AutoMailId("${messageId.localPart}@$unsubscribeDomainOverride")
                }

                val emailDBO = EmailDBO(
                    recipient = recipientEmail,
                    transaction = transaction,
                    messageId = messageId,
                    metaData = recipient.metadataMap,
                    autoMailId = autoMailId
                )
                emailDAO.save(emailDBO)

                val pair = when (request.bodyCase) {
                    OutgoingEmail.BodyCase.STATIC -> getStaticBody(
                        request = request,
                        sendAt = requestedSendTime,
                        emailDBO = emailDBO
                    )

                    OutgoingEmail.BodyCase.TEMPLATE -> getTemplateBody(
                        request = request,
                        sendAt = requestedSendTime,
                        recipient = recipient,
                        emailDBO = emailDBO
                    )

                    else -> throw StatusRuntimeException(Status.INVALID_ARGUMENT)
                }
                emailDBO.subject = pair.first
                emailDBO.body = pair.second
                emailDAO.save(emailDBO)
            }
        }
        val refreshedTransaction = emailSendTransactionDAO.get(transaction.id)!!
        for (emailDBO in refreshedTransaction.emails) {
            LOGGER.trace { "Queueing Email(${emailDBO.id}) for AccountId(${auth.account.id}) sending it at $requestedSendTime" }
            mqSender.queueJob(
                jobMessageFactory.createSendEmailJob(
                    requestedSendTime = requestedSendTime,
                    emailId = emailDBO.id,
                    mailDomain = auth.account.mailDomain
                )
            )

            send(
                EmailQueueStatus.newBuilder()
                    .setEmailId(emailDBO.id.toGrpcInternal())
                    .setQueued(Queued.getDefaultInstance())
                    .setTransactionId(transaction.id.toGrpcInternal())
                    .setRecipient(emailDBO.recipient.toGrpcInternal())
                    .build()
            )
        }
    }.buffer(UNLIMITED)

    private fun getTemplateBody(
        request: OutgoingEmail,
        recipient: EmailRecipient,
        emailDBO: EmailDBO,
        sendAt: Instant
    ): Pair<String, String> {
        val template = request.template
        if (template.htmlTemplate.isBlank() && template.plaintextTemplate.isBlank()) {
            throw IllegalArgumentException("Needs at least one template (plaintext or html)")
        }

        val globalVarsStruct =
            if (template.hasGlobalVars()) {
                template.globalVars
            } else {
                Struct.getDefaultInstance()
            }
        val globalVars = unwrapStruct(globalVarsStruct)

        val recipientVars = unwrapStruct(recipient.templateVars)

        val htmlBody = mergeTemplate(template.htmlTemplate, globalVars, recipientVars)
        val plaintextBody = mergeTemplate(template.plaintextTemplate, globalVars, recipientVars)

        val subject = mergeTemplate(template.subject, globalVars, recipientVars)
        return subject to wrapAndPackBody(
            request = request,
            sendAt = sendAt,
            htmlBody = htmlBody,
            plaintextBody = plaintextBody.emptyToNull(),
            subject = subject,
            emailDBO = emailDBO
        )
    }

    private fun getStaticBody(
        request: OutgoingEmail,
        emailDBO: EmailDBO,
        sendAt: Instant
    ): Pair<String, String> {
        val static = request.static
        val htmlBody = static.htmlBody.emptyToNull()
        val plaintextBody = static.plaintextBody.emptyToNull()

        return static.subject to wrapAndPackBody(
            request = request,
            sendAt = sendAt,
            htmlBody = htmlBody,
            plaintextBody = plaintextBody,
            subject = static.subject,
            emailDBO = emailDBO
        )
    }

    private fun unwrapStruct(struct: Struct): HashMap<String, Any> {
        return dbObjectMapper.objectMapper.readValue(
            JsonFormat.printer().print(struct),
            object : TypeReference<HashMap<String, Any>>() {}
        )
    }

    // Store the request for later debugging
    private fun getRequestData(request: OutgoingEmail) =
        dbObjectMapper.objectMapper.readValue(PRINTER.print(request)!!, ObjectNode::class.java)!!

    private fun mergeTemplate(
        template: String,
        vararg scopes: HashMap<String, Any>
    ) = mustacheCompiler
        .compile(template)
        .execute(scopes)

    private fun wrapAndPackBody(
        request: OutgoingEmail,
        sendAt: Instant,
        htmlBody: String?,
        plaintextBody: String?,
        subject: String,
        emailDBO: EmailDBO
    ): String {
        if (htmlBody == null && plaintextBody == null) {
            throw IllegalArgumentException("Needs at least one of plaintext or html")
        }

        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        message.setFrom(request.from.toNamedEmail().toAddress())
        message.setSubject(subject, "UTF-8")
        val sendDate = ZonedDateTime.ofInstant(
            sendAt.coerceAtLeast(clock.instant()),
            request.timeZone.takeUnless { request.timeZone.isEmpty() }
                ?.let { ZoneId.of(it) }
                ?: UTC
        )
        message.setHeader("Date", mailDateFormat.format(sendDate))

        message.setRecipients(Message.RecipientType.TO, arrayOf(emailDBO.recipientNamedEmail.toAddress()))

        if (request.replyTo.email.isNotBlank()) {
            val replyToMailDomain = request.replyTo.toEmail().domain.mailDomain
            val fromMailDomain = request.from.toNamedEmail().address.domain.mailDomain
            if (replyToMailDomain != fromMailDomain) {
                throw IllegalArgumentException("Reply-to email domain $replyToMailDomain not same as from email domain $fromMailDomain")
            }
            message.replyTo = arrayOf(InternetAddress(request.replyTo.email))
        }

        val unsubscribeUri = createUnsubscribeOneClickPostLink(emailDBO, request.unsubscribeRedirectUri)
        addListAndAbuseHeaders(
            message = message,
            emailDBO = emailDBO,
            unsubscribeUri = unsubscribeUri
        )

        for ((key, value) in request.headersMap) {
            message.addHeader(key, value)
        }

        val mixed = MimeMultipart("mixed")

        mixed.addMultipart("alternative") {
            val htmlDoc = parseHTML(
                htmlBody = htmlBody,
                plaintextBody = plaintextBody ?: "",
                inlineCSS = request.inlineCss
            ).apply {
                outputSettings()
                    .indentAmount(0)
                    .prettyPrint(false)
            }

            replaceLinks(htmlDoc, emailDBO, unsubscribeUri)
            injectTrackingPixel(htmlDoc, emailDBO)

            val plainText = { getPlainText(htmlDoc) }

            addBodyPart {
                setContent(plaintextBody ?: plainText(), "text/plain; charset=utf-8")
                setHeader("Content-Transfer-Encoding", "quoted-printable")
            }

            addMultipart("related") {
                addBodyPart {
                    setContent(htmlDoc.outerHtml(), "text/html; charset=utf-8")
                    setHeader("Content-Transfer-Encoding", "quoted-printable")
                }

                for (relatedAttachment in request.relatedAttachmentsList) {
                    addBodyPart {
                        require(relatedAttachment.contentId.isNotBlank()) {
                            "Content id of related attachment cannot be empty"
                        }
                        contentID = relatedAttachment.contentId
                        dataHandler = DataHandler(ByteArrayDataSource(relatedAttachment.data.toByteArray(), relatedAttachment.contentType))
                        if (relatedAttachment.fileName.isNotBlank()) {
                            fileName = relatedAttachment.fileName
                        }
                    }
                }
            }
        }

        for (attachment in request.attachmentsList) {
            mixed.addBodyPart {
                val content = attachment.data.toByteArray()

                dataHandler = DataHandler(ByteArrayDataSource(content, attachment.contentType))
                if (attachment.fileName.isNotBlank()) {
                    fileName = attachment.fileName
                }
            }
        }

        message.setContent(mixed)
        message.saveChanges()
        message.setHeader("Message-ID", "<${emailDBO.messageId}>")

        val result = ByteArrayOutputStream()
        message.writeTo(result)
        return Regex("^\\.$", RegexOption.MULTILINE)
            .replace(
                input = result.toString(StandardCharsets.UTF_8.name()),
                transform = { "=2E" }
            )
    }

    private fun addListAndAbuseHeaders(message: MimeMessage, emailDBO: EmailDBO, unsubscribeUri: URI) {
        // TODO Headers to set:
        // List-Help (IMPORTANT): <https://support.google.com/a/example.com/bin/topic.py?topic=25838>, <mailto:debug+help@example.com>

        val unsubscribeEmail = Email("unsub-${emailDBO.autoMailId}")
        val bounceReturnPathEmail = Email("bounce-${emailDBO.autoMailId}")
        // MUST have a valid DomainKeys Identified Mail (DKIM) signature that covers at least the List-Unsubscribe and List-Unsubscribe-Post headers
        message.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click")
        message.addHeader("List-Unsubscribe", "<mailto:$unsubscribeEmail>, <$unsubscribeUri>")
        message.addHeader("Return-Path", bounceReturnPathEmail.address)
        // TODO Abuse-email should be system-wide config parameter
        message.addHeader("X-Report-Abuse", "Please forward a copy of this message, including all headers, to abuse@${emailDBO.transaction.fromEmail.domain}")
        val abuseUrl = JerseyUriBuilder(baseUrl(emailDBO))
            .paths("abuse/{0}")
            .build(emailDBO.autoMailId.autoMailId)
        message.addHeader("X-Report-Abuse", "You can also report abuse here: $abuseUrl")
        message.addHeader("X-Tachikoma-User", emailDBO.transaction.authentication.id.toString())
    }

    /**
     * Css inliner for email, inspiration taken from
     * http://stackoverflow.com/questions/4521557/automatically-convert-style-sheets-to-inline-style
     *
     * Thanks to Hecho por Grekz
     */
    @TestOnly
    fun parseHTML(htmlBody: String?, plaintextBody: String, inlineCSS: Boolean): Document {
        if (htmlBody == null || !inlineCSS) {
            return Jsoup.parse(htmlBody ?: "<html><body>$plaintextBody</body></html>")
        }
        val style = "style"
        val htmlDocument = Jsoup.parse(htmlBody)
        val els = htmlDocument.select(style)
        val selectorMap: LinkedHashMap<String, String> = LinkedHashMap()
        val inlineProps: HashMap<Int, String> = HashMap()
        for (e in els) {
            val styleRules = e.allElements[0].data().replace("\n".toRegex(), "").trim { it <= ' ' }
            val delims = "{}"
            val st = StringTokenizer(styleRules, delims)

            while (st.countTokens() > 1) {
                val selector = st.nextToken().trim().replace(Regex(" +"), " ")
                val properties = st.nextToken().trim().replace(Regex(" +"), " ").replace("\"", "'")
                selectorMap[selector] = properties
            }
        }

        selectorMap.forEach { elem ->
            var selector = elem.key
            val properties = elem.value
            var ignoreSelector = false

            // Process selectors such as "a:hover"
            if (selector.indexOf(":") > 0) {
                selector = selector.substring(0, selector.indexOf(":"))
            }
            if (selector.isEmpty()) {
                ignoreSelector = true
            }
            if (selector.contains("*") || selector.contains("@")) {
                ignoreSelector = true
            }
            if (!ignoreSelector) {
                val selectedElements = htmlDocument.select(selector)
                for (selElem in selectedElements) {
                    if (!inlineProps.containsKey(selElem.hashCode())) {
                        inlineProps[selElem.hashCode()] = selElem.attr(style)
                    }
                    val oldProperties = selElem.attr(style)
                    selElem.attr(
                        style,
                        if (oldProperties.isNotEmpty()) {
                            val inlineProperties = inlineProps[selElem.hashCode()]!!
                            concatenateProperties(oldProperties, properties, inlineProperties)
                        } else {
                            properties
                        }
                    )
                }
            }
        }
        return htmlDocument
    }

    private fun MutableMap<String, String>.setCssProperties(props: String) {
        props.split(";")
            .filter { it.isNotBlank() }
            .associateTo(this) { el ->
                val (selector, prop) = el.split(":")
                selector.trim() to prop.trim()
            }
    }

    private fun concatenateProperties(
        oldProps: String,
        newProps: String,
        inlineProps: String
    ): String {
        val resultingProps = sortedMapOf<String, String>()

        resultingProps.setCssProperties(oldProps)
        resultingProps.setCssProperties(newProps)
        resultingProps.setCssProperties(inlineProps)

        return resultingProps.map { "${it.key}: ${it.value}" }.joinToString("; ")
    }

    @TestOnly
    fun createUnsubscribeOneClickPostLink(email: EmailDBO, unsubscribeRedirectUri: String): URI {
        val unsubscribeData = UnsubscribeData.newBuilder()
            .setEmailId(email.id.toGrpcInternal())
            .setRedirectUrl(unsubscribeRedirectUri)
            .build()
        val unsubscribeUrl = unsubscribeDecoderImpl.createUrl(unsubscribeData)

        return JerseyUriBuilder(baseUrl(email))
            .paths("unsubscribe/{0}")
            .build(unsubscribeUrl)
    }

    @TestOnly
    fun createUnsubscribeClickLink(email: EmailDBO, redirectUri: URI? = null): URI {
        val unsubscribeData = UnsubscribeData.newBuilder()
            .setEmailId(email.id.toGrpcInternal())
            .setRedirectUrl(redirectUri?.toString() ?: "")
            .build()
        val unsubscribeUrl = unsubscribeDecoderImpl.createUrl(unsubscribeData)

        return JerseyUriBuilder(baseUrl(email))
            .paths("unsubscribe/{0}")
            .build(unsubscribeUrl)
    }

    @TestOnly
    fun createTrackingLink(email: EmailDBO, originalUri: String): URI {
        val trackingData = UrlTrackingData.newBuilder()
            .setEmailId(email.id.toGrpcInternal())
            .setRedirectUrl(originalUri)
            .build()
        val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

        return JerseyUriBuilder(baseUrl(email))
            .paths("c/{0}")
            .build(trackingUrl)
    }

    private fun replaceLinks(doc: Document, email: EmailDBO, unsubscribeUri: URI) {
        val links = doc.select("a[href]")
        for (link in links) {
            val originalUri = link.attr("href")
            if (originalUri == "*|UNSUB|*") {
                link.attr("href", unsubscribeUri.toString())
            } else if (originalUri.startsWith("http://") || originalUri.startsWith("https://")) {
                link.attr(
                    "href",
                    createTrackingLink(email, originalUri).toString()
                )
            }
        }
    }

    private fun injectTrackingPixel(doc: Document, email: EmailDBO) {
        val trackingData = UrlTrackingData.newBuilder()
            .setEmailId(email.id.toGrpcInternal())
            .build()
        val trackingUrl = trackingDecoderImpl.createUrl(trackingData)

        val trackingUri = JerseyUriBuilder(baseUrl(email))
            .paths("t/{0}")
            .build(trackingUrl)

        val trackingPixel = Element("img")
        trackingPixel.attr("src", trackingUri.toString())
        trackingPixel.attr("height", "1")
        trackingPixel.attr("width", "1")
        doc.body().appendChild(trackingPixel)
    }

    @Suppress("SameParameterValue")
    private fun MimeMultipart.addMultipart(subtype: String, block: MimeMultipart.() -> Unit) =
        MimeMultipart(subtype).apply(block)
            .also { multipart ->
                addBodyPart {
                    setContent(multipart)
                }
            }

    private fun MimeMultipart.addBodyPart(block: MimeBodyPart.() -> Unit): MimeMultipart {
        addBodyPart(MimeBodyPart().apply(block))
        return this
    }

    companion object {
        private val LOGGER = logger()
        private val PRINTER = JsonFormat.printer()!!
        private val mailDateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z (z)")
    }
}

fun NamedEmail.toAddress(): InternetAddress =
    InternetAddress(address.address, name)
