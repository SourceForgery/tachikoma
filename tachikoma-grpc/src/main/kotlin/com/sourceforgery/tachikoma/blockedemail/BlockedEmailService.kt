package com.sourceforgery.tachikoma.blockedemail

import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class BlockedEmailService
@Inject
private constructor(
    private val authenticationDAO: AuthenticationDAO,
    private val blockedEmailDAO: BlockedEmailDAO
) {
    fun getBlockedEmails(responseObserver: StreamObserver<BlockedEmail>, authenticationId: AuthenticationId) {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        val blockedEmails = blockedEmailDAO.getBlockedEmails(authenticationDBO.account)

        blockedEmails.forEach {

            val blockedEmail = BlockedEmail
                .newBuilder()
                .setFromEmail(it.fromEmail.toGrpcInternal())
                .setRecipientEmail(it.recipientEmail.toGrpcInternal())
                .setBlockedReason(it.blockedReason.toGrpc())
                .build()

            responseObserver.onNext(blockedEmail)
        }
    }

    fun removeBlockedEmail(request: RemoveBlockedEmailRequest, authenticationId: AuthenticationId) {
        val authenticationDBO = authenticationDAO.getActiveById(authenticationId)!!

        blockedEmailDAO.unblock(authenticationDBO.account, request.fromEmail.toEmail(), request.recipientEmail.toEmail())
    }
}
