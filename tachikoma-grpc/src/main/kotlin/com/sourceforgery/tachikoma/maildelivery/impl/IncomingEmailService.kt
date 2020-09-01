package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.ReceivedBetween
import com.sourceforgery.tachikoma.database.objects.ReceiverEmailContains
import com.sourceforgery.tachikoma.database.objects.ReceiverNameContains
import com.sourceforgery.tachikoma.database.objects.SenderEmailContains
import com.sourceforgery.tachikoma.database.objects.SenderNameContains
import com.sourceforgery.tachikoma.database.objects.SubjectContains
import com.sourceforgery.tachikoma.database.objects.id
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.CONTAINS_RECEIVER_EMAIL
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.CONTAINS_RECEIVER_NAME
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.CONTAINS_SENDER_EMAIL
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.CONTAINS_SENDER_NAME
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.CONTAINS_SUBJECT
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.FILTER_NOT_SET
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailSearchFilter.FilterCase.RECEIVED_WITHIN
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.HeaderLine
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailAttachment
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.maildelivery.getPlainText
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.onlyIf
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.time.Instant
import java.util.Properties
import javax.activation.DataHandler
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.internet.ContentType
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.apache.logging.log4j.kotlin.logger
import org.jsoup.Jsoup
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class IncomingEmailService(override val di: DI) : DIAware {
    private val incomingEmailDAO: IncomingEmailDAO by instance()
    private val mqSequenceFactory: MQSequenceFactory by instance()

    fun getIncomingEmail(
        incomingEmailId: IncomingEmailId,
        accountId: AccountId,
        parameters: IncomingEmailParameters
    ): IncomingEmail? =
        incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
            ?.toGrpc(parameters)

    private fun IncomingEmailDBO.toGrpc(parameters: IncomingEmailParameters): IncomingEmail {
        val parsedMessage by lazy {
            val session = Session.getDefaultInstance(Properties())
            MimeMessage(session, ByteArrayInputStream(body))
        }
        return IncomingEmail.newBuilder()
            .setIncomingEmailId(id.toGrpc())
            .setSubject(subject)
            .setTo(NamedEmail(receiverEmail, receiverName).toGrpc())
            .setFrom(NamedEmail(fromEmail, fromName).toGrpc())
            .onlyIf(parameters.includeMessageParsedBodies) {
                includeParsedBodies(parsedMessage)
            }
            .onlyIf(parameters.includeMessageAttachments) {
                includeAttachments(parsedMessage)
            }
            .onlyIf(parameters.includeMessageHeader) {
                parsedMessage.allHeaders
                    .asSequence()
                    .map {
                        HeaderLine.newBuilder()
                            .setName(it.name)
                            .setBody(it.value)
                    }
                    .forEach { addMessageHeader(it) }
            }
            .onlyIf(parameters.includeMessageWholeEnvelope) {
                messageWholeEnvelope = ByteString.copyFrom(body)
            }
            .build()
    }

    private fun IncomingEmail.Builder.includeAttachments(parsedMessage: MimeMessage) {
        parsedMessage.unroll { _, _, _ -> true }
            .map { (_, singleBody) ->
                IncomingEmailAttachment.newBuilder()
                    .setContentType(singleBody.contentType)
                    .addAllHeaders(
                        singleBody.allHeaders
                            .asSequence()
                            .map {
                                HeaderLine.newBuilder()
                                    .setName(it.name)
                                    .setBody(it.value)
                                    .build()
                            }.toList()
                    )
                    .setFileName(singleBody.fileName ?: "")
                    .apply {
                        when (val content = singleBody.content) {
                            is String -> dataString = content
                            is DataHandler -> dataBytes = content.inputStream.use { ByteString.readFrom(it) }
                            else -> dataBytes = singleBody.inputStream.use { ByteString.readFrom(it) }
                        }
                    }
                    .build()
            }.forEach {
                addMessageAttachments(it)
            }
    }

    private fun Part.charset() =
        try {
            ContentType(contentType).getParameter("charset")
                ?.let { Charset.forName(it) }
                ?: Charsets.ISO_8859_1
        } catch (e: UnsupportedCharsetException) {
            LOGGER.info { "Detected unsupported charset: $contentType" }
            Charsets.ISO_8859_1
        }

    private fun Part.text() =
        when (val content = content) {
            is String -> content
            else -> InputStreamReader(inputStream, charset())
                .use { it.readText() }
        }

    private fun IncomingEmail.Builder.includeParsedBodies(parsedMessage: MimeMessage) {

        val allAlternatives = parsedMessage
            // Only allows two categories, either:
            // * the root part, ie the first in every
            // * in "multipart/alternative"
            // Caveat: This actually supports multiparts in multiparts,
            // but I have no idea if the standard does that.
            .unroll { parent, idx, _ -> idx == 0 || parent.isMultipartAlternative }
            .map { (_, part) -> part }
            .toList()

        messageHtmlBody = allAlternatives
            .firstOrNull { TEXT_HTML.match(it.contentType) }
            ?.text()
            ?: ""

        messageTextBody = allAlternatives
            .firstOrNull { TEXT_PLAIN.match(it.contentType) }
            ?.text()
            ?: let { messageHtmlBody.stripHtml() }
    }

    private val Multipart.isMultipartAlternative: Boolean
        get() = MULTIPART_ALTERNATIVE.match(contentType)

    private fun Multipart.bodyPartsWithIndex() = (0 until count)
        .asSequence()
        .map { it to getBodyPart(it) }

    // Not internal function because Kotlin 1.3.72 w/ coroutine 1.3.6 cannot build it
    suspend fun SequenceScope<Pair<Int, Part>>.recursive(idx: Int, body: Part, pathSelector: (Multipart, Int, Part) -> Boolean) {
        when (val content = body.content) {
            is Multipart -> {
                content.bodyPartsWithIndex()
                    .filter { (idx, part) -> pathSelector(content, idx, part) }
                    .forEach { (idx, part) ->
                        recursive(idx, part, pathSelector)
                    }
            }
            else -> yield(idx to body)
        }
    }

    /** Recursively get all bodies (if pathSelector allows it)
     * @param pathSelector filters both which paths to go down, and what to collect. Ie, if not accepting multiparts,
     *  the map will only contain the direct children of the Part-receiver.
     * **/
    private fun Part.unroll(pathSelector: (Multipart, Int, Part) -> Boolean): Sequence<Pair<Int, Part>> {
        return sequence {
            recursive(0, this@unroll, pathSelector)
        }
    }

    fun streamIncomingEmails(
        authenticationId: AuthenticationId,
        mailDomain: MailDomain,
        accountId: AccountId,
        parameters: IncomingEmailParameters
    ): Flow<IncomingEmail> =
        mqSequenceFactory.listenForIncomingEmails(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId
        ).mapNotNull {
            val incomingEmailId = IncomingEmailId(it.incomingEmailMessageId)
            val email = incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
            if (email != null) {
                email.toGrpc(parameters)
            } else {
                LOGGER.warn { "Could not find email with id $incomingEmailId" }
                null
            }
        }

    fun searchIncomingEmails(filter: List<EmailSearchFilter>, accountId: AccountId, parameters: IncomingEmailParameters): Flow<IncomingEmail> {
        val dbFilter = filter
            .map { it.convert() }
        return incomingEmailDAO.searchIncomingEmails(
            accountId = accountId,
            filter = dbFilter
        )
            .map { it.toGrpc(parameters) }
    }

    fun EmailSearchFilter.convert() =
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (filterCase) {
            CONTAINS_SUBJECT -> SubjectContains(containsSubject)
            CONTAINS_SENDER_NAME -> SenderNameContains(containsSenderName)
            CONTAINS_SENDER_EMAIL -> SenderEmailContains(containsSenderEmail)
            CONTAINS_RECEIVER_NAME -> ReceiverNameContains(containsReceiverName)
            CONTAINS_RECEIVER_EMAIL -> ReceiverEmailContains(containsReceiverEmail)
            RECEIVED_WITHIN -> ReceivedBetween(
                after = receivedWithin.after
                    .takeIf { it.seconds != 0L }
                    ?.toInstant()
                    ?: Instant.EPOCH,
                before = receivedWithin.before
                    .takeIf { it.seconds != 0L }
                    ?.toInstant()
                    ?: Instant.MAX
            )
            FILTER_NOT_SET -> error("Must set filter")
        }

    private fun String.stripHtml(): String = getPlainText(Jsoup.parse(this))

    companion object {
        private val TEXT_HTML = ContentType("text/html")
        private val TEXT_PLAIN = ContentType("text/plain")
        private val MULTIPART_ALTERNATIVE = ContentType("multipart/alternative")
        private val LOGGER = logger()
    }
}
