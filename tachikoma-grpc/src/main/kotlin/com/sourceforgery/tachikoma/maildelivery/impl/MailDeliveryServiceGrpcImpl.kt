package com.sourceforgery.tachikoma.maildelivery.impl

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.exceptions.NotFoundException
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailQueueStatus
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.GetIncomingEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.IncomingEmailParameters
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.MailDeliveryServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.OutgoingEmail
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.SearchIncomingEmailsRequest
import com.sourceforgery.tachikoma.identifiers.IncomingEmailId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class MailDeliveryServiceGrpcImpl(override val di: DI) : MailDeliveryServiceGrpcKt.MailDeliveryServiceCoroutineImplBase(), DIAware {

    private val mailDeliveryService: MailDeliveryService by instance()
    private val incomingEmailService: IncomingEmailService by instance()
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getIncomingEmails(request: Empty): Flow<IncomingEmail> =
        streamIncomingEmails(IncomingEmailParameters.getDefaultInstance())

    override fun streamIncomingEmails(request: IncomingEmailParameters) = flow<IncomingEmail> {
        val auth = authentication()
        auth.requireFrontend()
        LOGGER.info { "Connected, user ${auth.authenticationId} getting incoming mails from ${auth.mailDomain}" }
        incomingEmailService.streamIncomingEmails(
            authenticationId = auth.authenticationId,
            mailDomain = auth.mailDomain,
            accountId = auth.accountId,
            parameters = request
        )
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

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

    companion object {
        private val LOGGER = logger()
    }
}
