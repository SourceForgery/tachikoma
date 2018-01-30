package com.sourceforgery.tachikoma.blockedemail

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.RemoveBlockedEmailRequest
import com.sourceforgery.tachikoma.grpc.frontend.toEmail
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import com.sourceforgery.tachikoma.grpc.frontend.toGrpcInternal
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class BlockedEmailService
@Inject
private constructor(
        private val authentication: Authentication,
        private val authenticationDAO: AuthenticationDAO,
        private val blockedEmailDAO: BlockedEmailDAO
) {
    fun getBlockedEmails(responseObserver: StreamObserver<BlockedEmail>) {
        authentication.requireFrontend()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

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

    fun removeBlockedEmail(request: RemoveBlockedEmailRequest) {
        authentication.requireFrontend()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        blockedEmailDAO.unblock(authenticationDBO.account, request.fromEmail.toEmail(), request.recipientEmail.toEmail())
    }
}
