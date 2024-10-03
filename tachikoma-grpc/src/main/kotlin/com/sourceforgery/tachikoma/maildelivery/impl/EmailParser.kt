package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.HeaderLine
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailAttachment
import com.sourceforgery.tachikoma.maildelivery.getPlainText
import jakarta.activation.DataHandler
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.ContentType
import jakarta.mail.internet.MimeMessage
import org.apache.logging.log4j.kotlin.logger
import org.jsoup.Jsoup
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException

object EmailParser {
    private val TEXT_HTML = ContentType("text/html")
    private val TEXT_PLAIN = ContentType("text/plain")
    private val MULTIPART_ALTERNATIVE = ContentType("multipart/alternative")
    private val LOGGER = logger()

    fun IncomingEmail.Builder.includeAttachments(parsedMessage: MimeMessage) {
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
                            }.toList(),
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

    fun MimeMessage.parseBodies(): Bodies {
        // Only allows two categories, either:
        // * the root part, ie the first in every
        // * in "multipart/alternative"
        // Caveat: This actually supports multipart in multipart,
        // but I have no idea if the standard does that.
        val allAlternatives =
            unroll { parent, idx, _ -> idx == 0 || parent.isMultipartAlternative }
                .map { (_, part) -> part }
                .toList()

        val messageHtmlBody =
            allAlternatives
                .firstOrNull { TEXT_HTML.match(it.contentType) }
                ?.text()
                ?: ""

        val messageTextBody =
            allAlternatives
                .firstOrNull { TEXT_PLAIN.match(it.contentType) }
                ?.text()
                ?: let { messageHtmlBody.stripHtml() }
        return Bodies(
            htmlBody = messageHtmlBody,
            plainTextBody = messageTextBody,
        )
    }

    private val Multipart.isMultipartAlternative: Boolean
        get() = MULTIPART_ALTERNATIVE.match(contentType)

    /** Recursively get all bodies (if pathSelector allows it)
     * @param pathSelector filters both which paths to go down, and what to collect. Ie, if not accepting multiparts,
     *  the map will only contain the direct children of the Part-receiver.
     * **/
    private fun Part.unroll(pathSelector: (Multipart, Int, Part) -> Boolean): Sequence<Pair<Int, Part>> {
        return sequence {
            recursive(0, this@unroll, pathSelector)
        }
    }

    private fun Part.charset() =
        try {
            ContentType(contentType).getParameter("charset")
                ?.let { Charset.forName(it) }
                ?: Charsets.ISO_8859_1
        } catch (_: UnsupportedCharsetException) {
            LOGGER.info { "Detected unsupported charset: $contentType" }
            Charsets.ISO_8859_1
        }

    private fun Part.text() =
        when (val content = content) {
            is String -> content
            else ->
                InputStreamReader(inputStream, charset())
                    .use { it.readText() }
        }

    private fun Multipart.bodyPartsWithIndex() =
        (0 until count)
            .asSequence()
            .map { it to getBodyPart(it) }

    // Not internal function because Kotlin 1.3.72 w/ coroutine 1.3.6 cannot build it
    suspend fun SequenceScope<Pair<Int, Part>>.recursive(
        idx: Int,
        body: Part,
        pathSelector: (Multipart, Int, Part) -> Boolean,
    ) {
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

    private fun String.stripHtml(): String = getPlainText(Jsoup.parse(this))
}

data class Bodies(
    val htmlBody: String,
    val plainTextBody: String,
)
