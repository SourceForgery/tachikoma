package com.sourceforgery.tachikoma.common

import java.io.ByteArrayInputStream
import java.util.Properties
import javax.mail.Address
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import org.kodein.di.DI
import org.kodein.di.DIAware

class ExtractEmailMetadata(override val di: DI) : DIAware {
    fun extract(message: ByteArray): EmailMetadata {
        val mimeMessage = MimeMessage(Session.getDefaultInstance(Properties()), ByteArrayInputStream(message))
        return EmailMetadata(
            from = mimeMessage.from.parseNamedEmails(),
            replyTo = mimeMessage.replyTo.parseNamedEmails(),
            to = mimeMessage.getRecipients(Message.RecipientType.TO).parseNamedEmails(),
            cc = mimeMessage.getRecipients(Message.RecipientType.CC).parseNamedEmails()
        )
    }
}

data class EmailMetadata(
    val from: List<NamedEmail>,
    val replyTo: List<NamedEmail>,
    val to: List<NamedEmail>,
    val cc: List<NamedEmail>
)

internal fun Array<Address>.parseNamedEmails(): List<NamedEmail> =
    asSequence()
        .filterIsInstance<InternetAddress>()
        .map { NamedEmail(it.address, it.personal) }
        .toList()
