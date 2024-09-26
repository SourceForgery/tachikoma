package com.sourceforgery.tachikoma.blockedemail

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmailServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class BlockedEmailServiceGrpcImpl(
    override val di: DI,
) : BlockedEmailServiceGrpcKt.BlockedEmailServiceCoroutineImplBase(), DIAware {
    private val authentication: () -> Authentication by provider()
    private val blockedEmailService: BlockedEmailService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()

    override fun getBlockedEmails(request: Empty): Flow<BlockedEmail> =
        flow {
            val auth = authentication()
            auth.requireFrontend()
            emitAll(blockedEmailService.getBlockedEmails(auth.authenticationId))
        }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

    override suspend fun removeBlockedEmail(request: RemoveBlockedEmailRequest): Empty {
        try {
            val auth = authentication()
            auth.requireFrontend()
            blockedEmailService.removeBlockedEmail(request, auth.authenticationId)
            return Empty.getDefaultInstance()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
    }
}
