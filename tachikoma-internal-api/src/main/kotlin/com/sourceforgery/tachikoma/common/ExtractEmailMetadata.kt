package com.sourceforgery.tachikoma.common

import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.io.ByteArrayInputStream
import java.util.Properties
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware

class ExtractEmailMetadata(override val di: DI) : DIAware {
    fun extract(message: ByteArray): EmailMetadata {
        val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), ByteArrayInputStream(message))
        return try {
            EmailMetadata(
                from = mimeMessage.from
                    ?.parseNamedEmails()
                    ?: emptyList(),
                replyTo = mimeMessage.replyTo
                    ?.parseNamedEmails()
                    ?: emptyList(),
                to = mimeMessage.getRecipients(Message.RecipientType.TO)
                    ?.parseNamedEmails()
                    ?: emptyList(),
                cc = mimeMessage.getRecipients(Message.RecipientType.CC)
                    ?.parseNamedEmails()
                    ?: emptyList()
            )
        } catch (e: Exception) {
            LOGGER.warn(e) { "Could not parse emails in ${mimeMessage.messageID}" }
            EmailMetadata(listOf(), listOf(), listOf(), listOf())
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}

data class EmailMetadata
internal constructor(
    val from: List<NamedEmail>,
    val replyTo: List<NamedEmail>,
    val to: List<NamedEmail>,
    val cc: List<NamedEmail>
)

internal fun Array<Address>.parseNamedEmails(): List<NamedEmail> =
    asSequence()
        .filterIsInstance<InternetAddress>()
        .map { NamedEmail(it.address, it.personal ?: "") }
        .toList()
