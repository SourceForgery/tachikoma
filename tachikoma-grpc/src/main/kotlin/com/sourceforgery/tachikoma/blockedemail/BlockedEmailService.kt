package com.sourceforgery.tachikoma.blockedemail

import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

internal class BlockedEmailService(override val di: DI) : DIAware {
    private val authenticationDAO: AuthenticationDAO by instance()
    private val blockedEmailDAO: BlockedEmailDAO by instance()

    suspend fun getBlockedEmails(authenticationId: AuthenticationId): Flow<BlockedEmail> {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        return blockedEmailDAO.getBlockedEmails(authenticationDBO.account)
            .asFlow()
            .map {
                BlockedEmail
                    .newBuilder()
                    .setFromEmail(it.fromEmail.toGrpcInternal())
                    .setRecipientEmail(it.recipientEmail.toGrpcInternal())
                    .setBlockedReason(it.blockedReason.toGrpc())
                    .build()
            }
    }

    fun removeBlockedEmail(
        request: RemoveBlockedEmailRequest,
        authenticationId: AuthenticationId,
    ) {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        blockedEmailDAO.unblock(authenticationDBO.account, request.fromEmail.toEmail(), request.recipientEmail.toEmail())
    }
}
