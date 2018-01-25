package com.sourceforgery.tachikoma.blockedemail

import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.database.dao.AuthenticationDAO
import com.sourceforgery.tachikoma.database.dao.BlockedEmailDAO
import com.sourceforgery.tachikoma.grpc.frontend.EmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmail
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedEmails
import com.sourceforgery.tachikoma.grpc.frontend.toGrpc
import io.grpc.stub.StreamObserver
import javax.inject.Inject

internal class BlockedEmailService
@Inject
private constructor(
        private val authentication: Authentication,
        private val authenticationDAO: AuthenticationDAO,
        private val blockedEmailDAO: BlockedEmailDAO
) {
    fun getBlockedEmails(responseObserver: StreamObserver<BlockedEmails>) {
        authentication.requireFrontend()
        val authenticationDBO = authenticationDAO.getActiveById(authentication.authenticationId)!!

        val blockedEmails = blockedEmailDAO.getBlockedEmails(authenticationDBO.account)
        val blockedEmailsBuilder = BlockedEmails.newBuilder()

        blockedEmails.forEach {

            val fromEmail = EmailAddress
                    .newBuilder()
                    .setLocalPart(it.fromEmail.localPart)
                    .setMailDomain(it.fromEmail.domain.mailDomain)
                    .setAddress(it.fromEmail.address)
                    .build()

            val recipientEmail = EmailAddress
                    .newBuilder()
                    .setLocalPart(it.recipientEmail.localPart)
                    .setMailDomain(it.recipientEmail.domain.mailDomain)
                    .setAddress(it.recipientEmail.address)
                    .build()

            blockedEmailsBuilder.addBlockedEmail(BlockedEmail
                    .newBuilder()
                    .setFromEmail(fromEmail)
                    .setRecipientEmail(recipientEmail)
                    .setBlockedReason(it.blockedReason.toGrpc())
            )
        }

        responseObserver.onNext(blockedEmailsBuilder.build())
    }
}
