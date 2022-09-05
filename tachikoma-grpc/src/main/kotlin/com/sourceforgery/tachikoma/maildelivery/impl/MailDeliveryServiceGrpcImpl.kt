package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.exceptions.NotFoundException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatusList
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.GetIncomingEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailList
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailOrKeepAlive
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.SearchIncomingEmailsRequest
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import com.sourceforgery.tachikoma.withKeepAlive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.apache.commons.lang.RandomStringUtils
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class MailDeliveryServiceGrpcImpl(override val di: DI) :
    MailDeliveryServiceGrpcKt.MailDeliveryServiceCoroutineImplBase(), DIAware {

    private val mailDeliveryService: MailDeliveryService by instance()
    private val incomingEmailService: IncomingEmailService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val scope: TachikomaScope by instance()

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getIncomingEmails(request: Empty): Flow<IncomingEmail> =
        streamIncomingEmails(IncomingEmailParameters.getDefaultInstance())

    override fun streamIncomingEmails(request: IncomingEmailParameters) =
        streamIncomingEmailsWithKeepAlive(request)
            .filter { it.hasIncomingEmail() }
            .map { it.incomingEmail }

    override fun streamIncomingEmailsWithKeepAlive(request: IncomingEmailParameters) = channelFlow {
        val auth = authentication()
        auth.requireFrontend()
        LOGGER.info { "Connected, user ${auth.authenticationId} getting incoming mails from ${auth.mailDomain}" }
        withKeepAlive(
            IncomingEmailOrKeepAlive.newBuilder()
                .setKeepAlive(RandomStringUtils.randomAlphanumeric(1000))
                .build()
        )
        incomingEmailService.streamIncomingEmails(
            authenticationId = auth.authenticationId,
            mailDomain = auth.mailDomain,
            accountId = auth.accountId,
            parameters = request
        ).map {
            IncomingEmailOrKeepAlive.newBuilder()
                .setIncomingEmail(it)
                .build()
        }.collect {
            send(it)
        }
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }
        .buffer(Channel.RENDEZVOUS)

    override suspend fun getIncomingEmail(request: GetIncomingEmailRequest): IncomingEmail {
        try {
            val auth = authentication()
            auth.requireFrontend()
            return incomingEmailService.getIncomingEmail(
                incomingEmailId = IncomingEmailId(request.incomingEmailId.id),
                accountId = auth.accountId,
                parameters = request.parameters
            ) ?: throw NotFoundException("No email ${request.incomingEmailId.id} that ${auth.accountId} can access")
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }

    override fun searchIncomingEmails(request: SearchIncomingEmailsRequest): Flow<IncomingEmail> = flow {
        val auth = authentication()
        auth.requireFrontend()
        emitAll(
            incomingEmailService.searchIncomingEmails(
                filter = request.messageFilterList,
                accountId = auth.accountId,
                parameters = request.parameters
            )
        )
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

    override suspend fun searchIncomingEmailsUnary(request: SearchIncomingEmailsRequest): IncomingEmailList {
        val builder = IncomingEmailList.newBuilder()
        searchIncomingEmails(request)
            .collect { builder.addList(it) }
        return builder.build()
    }

    override fun sendEmail(request: OutgoingEmail): Flow<EmailQueueStatus> = flow {
        val auth = authentication()
        auth.requireFrontend()
        LOGGER.trace {
            val recipients = request.recipientsList.joinToString { it.namedEmail.email }
            "Starting sendEmail from ${request.from.email} to $recipients for AccountId(${auth.accountId})"
        }
        emitAll(
            mailDeliveryService.sendEmail(
                request = request,
                authenticationId = auth.authenticationId
            )
        )
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

    override suspend fun sendEmailUnary(request: OutgoingEmail): EmailQueueStatusList {
        val builder = EmailQueueStatusList.newBuilder()
        sendEmail(request)
            .collect { builder.addList(it) }
        LOGGER.error { "Done sending" }
        return builder.build()
    }

    companion object {
        private val LOGGER = logger()
    }
}
