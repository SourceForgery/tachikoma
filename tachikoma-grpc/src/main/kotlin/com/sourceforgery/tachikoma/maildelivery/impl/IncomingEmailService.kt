package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.ByteString
import com.sourceforgery.tachikoma.common.NamedEmail
import com.sourceforgery.tachikoma.database.dao.IncomingEmailDAO
import com.sourceforgery.tachikoma.database.objects.IncomingEmailDBO
import com.sourceforgery.tachikoma.database.objects.id
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
import io.grpc.stub.StreamObserver
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors
import javax.mail.internet.ContentType
import org.apache.james.mime4j.dom.BinaryBody
import org.apache.james.mime4j.dom.Body
import org.apache.james.mime4j.dom.Multipart
import org.apache.james.mime4j.dom.SingleBody
import org.apache.james.mime4j.dom.TextBody
import org.apache.james.mime4j.message.DefaultMessageBuilder
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
            DefaultMessageBuilder().parseMessage(ByteArrayInputStream(body))
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
                parsedMessage.header.fields
                    .map {
                        HeaderLine.newBuilder()
                            .setName(it.name)
                            .setBody(it.body)
                    }
                    .forEach { addMessageHeader(it) }
            }
            .onlyIf(parameters.includeMessageWholeEnvelope) {
                messageWholeEnvelope = ByteString.copyFrom(body)
            }
            .build()
    }

    private fun IncomingEmail.Builder.includeAttachments(parsedMessage: org.apache.james.mime4j.dom.Message) {
        parsedMessage.body.unroll()
            .map { singleBody ->
                IncomingEmailAttachment.newBuilder()
                    .setContentType(singleBody.parent.mimeType)
                    .addAllHeaders(
                        singleBody.parent.header
                            .asSequence()
                            .map {
                                HeaderLine.newBuilder()
                                    .setName(it.name)
                                    .setBody(it.body)
                                    .build()
                            }.toList()
                    )
                    .setFileName(singleBody.parent.filename ?: "")
                    .apply {
                        when (singleBody) {
                            is TextBody -> dataString = singleBody.reader.use { it.readText() }
                            is BinaryBody -> dataBytes = singleBody.inputStream.use { ByteString.readFrom(it) }
                        }
                    }
                    .build()
            }.forEach {
                addMessageAttachments(it)
            }
    }

    private fun IncomingEmail.Builder.includeParsedBodies(parsedMessage: org.apache.james.mime4j.dom.Message) {
        when (val content = parsedMessage.body) {
            is Multipart -> {
                val htmlBody = content
                    .getFirstTextBody(TEXT_HTML)
                    ?.let { textBody ->
                        textBody.reader.use { it.readText() }
                    }

                val textBody = content
                    .getFirstTextBody(TEXT_PLAIN)
                    ?.let { textBody ->
                        textBody.reader.use { it.readText() }
                    }
                    ?: let {
                        htmlBody?.let {
                            getPlainText(Jsoup.parse(it))
                        }
                    }
                messageHtmlBody = htmlBody ?: ""
                messageTextBody = textBody ?: ""
            }
            is TextBody -> {
                messageTextBody = content.reader.use { it.readText() }
            }
            else -> LOGGER.trace { "Couldn't find anything to put in parsed body for email Incoming Email Id $incomingEmailId" }
        }
    }

    /** Recursively get all non-multi-part bodies **/
    private fun Body.unroll(): Sequence<SingleBody> {
        suspend fun SequenceScope<SingleBody>.recursive(body: Body) {
            if (body is Multipart) {
                for (bodyPart in body.bodyParts) {
                    recursive(bodyPart.body)
                }
            } else if (body is SingleBody) {
                yield(body)
            }
        }

        return sequence {
            recursive(this@unroll)
        }
    }

    /**
     * This is NOT recursive and will only look at the first level of bodies.
     */
    private fun Body.getFirstTextBody(contentType: ContentType): TextBody? =
        unroll()
            .filter { contentType.match(it.parent.mimeType) }
            .filterIsInstance<TextBody>()
            .firstOrNull()

    fun streamIncomingEmails(
        responseObserver: StreamObserver<IncomingEmail>,
        authenticationId: AuthenticationId,
        mailDomain: MailDomain,
        accountId: AccountId,
        parameters: IncomingEmailParameters
    ) {
        val future = mqSequenceFactory.listenForIncomingEmails(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
            callback = {
                val incomingEmailId = IncomingEmailId(it.incomingEmailMessageId)
                val email = incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
                if (email != null) {
                    val incomingEmail = email.toGrpc(parameters)
                    responseObserver.onNext(incomingEmail)
                } else {
                    LOGGER.warn { "Could not find email with id $incomingEmailId" }
                }
            }
        )
        future.addListener(Runnable {
            responseObserver.onCompleted()
        }, responseCloser)
    }

    companion object {
        private val TEXT_HTML = ContentType("text/html")
        private val TEXT_PLAIN = ContentType("text/plain")
        private val responseCloser = Executors.newCachedThreadPool()
        private val LOGGER = logger()
    }
}
