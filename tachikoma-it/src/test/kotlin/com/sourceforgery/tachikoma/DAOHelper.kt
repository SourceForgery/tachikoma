package com.sourceforgery.tachikoma

import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.util.JsonFormat
import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.Email
import com.sourceforgery.tachikoma.common.EmailStatus
import com.sourceforgery.tachikoma.database.objects.AccountDBO
import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.EmailDBO
import com.sourceforgery.tachikoma.database.objects.EmailSendTransactionDBO
import com.sourceforgery.tachikoma.database.objects.EmailStatusEventDBO
import com.sourceforgery.tachikoma.database.objects.StatusEventMetaData
import com.sourceforgery.tachikoma.database.server.DBObjectMapper
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.identifiers.AutoMailId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.identifiers.MessageId
import io.ebean.Database
import java.time.Instant
import java.util.UUID
import org.apache.commons.lang3.RandomStringUtils
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class DAOHelper(override val di: DI) : DIAware {
    private val database: Database by instance()
    private val dbObjectMapper: DBObjectMapper by instance()

    fun createAuthentication(domain: String) =
        createAuthentication(MailDomain(domain))

    fun createAuthentication(domain: MailDomain): AuthenticationDBO {
        val accountDBO = AccountDBO(domain)
        database.save(accountDBO)

        val authenticationDBO = AuthenticationDBO(
            login = domain.mailDomain,
            encryptedPassword = UUID.randomUUID().toString(),
            apiToken = UUID.randomUUID().toString(),
            role = AuthenticationRole.BACKEND,
            account = accountDBO
        )
        database.save(authenticationDBO)

        return authenticationDBO
    }

    fun createEmailStatusEvent(
        authentication: AuthenticationDBO,
        from: Email,
        recipient: Email,
        emailStatus: EmailStatus,
        dateCreated: Instant? = null
    ): EmailStatusEventDBO {

        val outgoingEmail = OutgoingEmail.newBuilder().build()
        val jsonRequest = dbObjectMapper.objectMapper.readValue(PRINTER.print(outgoingEmail)!!, ObjectNode::class.java)!!

        val email = EmailDBO(
            recipient = recipient,
            recipientName = "Mr. Recipient",
            transaction = EmailSendTransactionDBO(
                jsonRequest = jsonRequest,
                fromEmail = from,
                authentication = authentication,
                metaData = emptyMap(),
                tags = emptyList()
            ),
            messageId = MessageId("${UUID.randomUUID()}@example.com"),
            autoMailId = AutoMailId("${UUID.randomUUID()}@example.net"),
            mtaQueueId = null,
            metaData = emptyMap()
        )
        database.save(email)

        val emailStatusEventDBO = EmailStatusEventDBO(
            emailStatus = emailStatus,
            email = email,
            metaData = StatusEventMetaData()
        )
        database.save(emailStatusEventDBO)
        dateCreated?.also {
            emailStatusEventDBO.dateCreated = it
            database.save(emailStatusEventDBO)
        }

        return emailStatusEventDBO
    }

    companion object {
        val PRINTER = JsonFormat.printer()!!
    }
}

fun Email.unique() = Email(domain, "localPart+${RandomStringUtils.randomAlphanumeric(10)}")
