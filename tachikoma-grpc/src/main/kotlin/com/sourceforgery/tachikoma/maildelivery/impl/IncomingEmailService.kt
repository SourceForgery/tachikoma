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
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.maildelivery.extractBodyFromPlaintextEmail
import com.sourceforgery.tachikoma.maildelivery.impl.EmailParser.includeAttachments
import com.sourceforgery.tachikoma.maildelivery.impl.EmailParser.parseBodies
import com.sourceforgery.tachikoma.mq.MQSequenceFactory
import com.sourceforgery.tachikoma.onlyIf
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Properties

class IncomingEmailService(override val di: DI) : DIAware {
    private val incomingEmailDAO: IncomingEmailDAO by instance()
    private val mqSequenceFactory: MQSequenceFactory by instance()

    fun getIncomingEmail(
        incomingEmailId: IncomingEmailId,
        accountId: AccountId,
        parameters: IncomingEmailParameters,
    ): IncomingEmail? =
        incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
            ?.toGrpc(parameters)

    private fun IncomingEmailDBO.toGrpc(parameters: IncomingEmailParameters): IncomingEmail {
        val parsedMessage by lazy {
            val session = Session.getDefaultInstance(Properties())
            MimeMessage(session, ByteArrayInputStream(body))
        }
        val parseBodies by lazy {
            parsedMessage.parseBodies()
        }
        @Suppress("DEPRECATION")
        return IncomingEmail.newBuilder()
            .setIncomingEmailId(id.toGrpc())
            .setSubject(subject)
            .setMailFromOld(NamedEmail(mailFrom, "").toGrpc())
            .setRecipientToOld(NamedEmail(recipient, "").toGrpc())
            .setRecipientTo(recipient.toGrpcInternal())
            .addAllFrom(fromEmails.map { it.toGrpc() })
            .addAllReplyTo(replyToEmails.map { it.toGrpc() })
            .addAllTo(toEmails.map { it.toGrpc() })
            .onlyIf(parameters.includeMessageParsedBodies) {
                messageHtmlBody = parseBodies.htmlBody
                messageTextBody = parseBodies.plainTextBody
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
            .onlyIf(parameters.includeExtractedMessageFromReplyChain) {
                extractedTextMessageFromReplyChain = extractBodyFromPlaintextEmail(parseBodies.plainTextBody, recipient)
            }
            .build()
    }

    fun streamIncomingEmails(
        authenticationId: AuthenticationId,
        mailDomain: MailDomain,
        accountId: AccountId,
        parameters: IncomingEmailParameters,
    ): Flow<IncomingEmail> =
        mqSequenceFactory.listenForIncomingEmails(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
        ).mapNotNull {
            val incomingEmailId = IncomingEmailId(it.incomingEmailMessageId)
            val email = incomingEmailDAO.fetchIncomingEmail(incomingEmailId, accountId)
            if (email != null) {
                LOGGER.trace { "Sending incoming email ($incomingEmailId) to AccountId ($accountId)" }
                email.toGrpc(parameters)
            } else {
                LOGGER.warn { "Could not find email with id $incomingEmailId" }
                null
            }
        }

    fun searchIncomingEmails(
        filter: List<EmailSearchFilter>,
        accountId: AccountId,
        parameters: IncomingEmailParameters,
    ): Flow<IncomingEmail> {
        val dbFilter =
            filter
                .map { it.convert() }
        return incomingEmailDAO.searchIncomingEmails(
            accountId = accountId,
            filter = dbFilter,
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
            RECEIVED_WITHIN ->
                ReceivedBetween(
                    after =
                        receivedWithin.after
                            .takeIf { it.seconds != 0L }
                            ?.toInstant()
                            ?: Instant.EPOCH,
                    before =
                        receivedWithin.before
                            .takeIf { it.seconds != 0L }
                            ?.toInstant()
                            ?: Instant.MAX,
                )
            FILTER_NOT_SET -> error("Must set filter")
        }

    companion object {
        private val LOGGER = logger()
    }
}
