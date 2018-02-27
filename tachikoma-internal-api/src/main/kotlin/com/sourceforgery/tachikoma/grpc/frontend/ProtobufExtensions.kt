package com.sourceforgery.tachikoma.grpc.frontend

import com.sourceforgery.tachikoma.common.AuthenticationRole
import com.sourceforgery.tachikoma.common.BlockedReason
import com.sourceforgery.tachikoma.grpc.frontend.auth.AuthRole
import com.sourceforgery.tachikoma.grpc.frontend.auth.WebTokenAuthData
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.FrontendUserRole
import com.sourceforgery.tachikoma.grpc.frontend.blockedemail.UserId
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.EmailRecipient
import com.sourceforgery.tachikoma.grpc.frontend.maildelivery.Rejected
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.EmailId

fun com.sourceforgery.tachikoma.common.Email.toGrpcInternal() =
        EmailAddress.newBuilder().setEmail(address).build()

fun EmailAddress.toEmail() =
        com.sourceforgery.tachikoma.common.Email(email)

fun grpcEmailInternal(emailAddress: String) =
        EmailAddress.newBuilder().setEmail(emailAddress).build()

fun com.sourceforgery.tachikoma.common.NamedEmail.toGrpcInternal() =
        NamedEmailAddress.newBuilder()
                .setEmail(address.address)
                .setName(name)
                .build()

fun NamedEmailAddress.toNamedEmail() =
        com.sourceforgery.tachikoma.common.NamedEmail(com.sourceforgery.tachikoma.common.Email(email), name)

fun EmailId.toGrpcInternal() =
        com.sourceforgery.tachikoma.grpc.frontend.EmailId.newBuilder().setId(emailId).build()

fun com.sourceforgery.tachikoma.grpc.frontend.EmailId.toEmailId() =
        EmailId(id)

fun com.sourceforgery.tachikoma.identifiers.EmailTransactionId.toGrpcInternal() =
        EmailTransactionId.newBuilder().setId(emailTransactionId).build()

fun WebTokenAuthData.toAccountId(): AccountId? {
    return if (accountId == 0L) {
        null
    } else {
        AccountId(accountId)
    }
}

fun WebTokenAuthData.toAuthenticationId(): AuthenticationId? {
    return if (userId == 0L) {
        null
    } else {
        AuthenticationId(userId)
    }
}

fun AuthenticationId.toUserId() =
        UserId.newBuilder().setId(authenticationId).build()

fun UserId.toAuthenticationId() =
        AuthenticationId(id)

fun EmailRecipient.toNamedEmail() =
        com.sourceforgery.tachikoma.common.NamedEmail(
                address = namedEmail.email,
                name = namedEmail.name
        )

fun String?.emptyToNull() =
        if (this == null || this.isEmpty()) {
            null
        } else {
            this
        }

fun BlockedReason.toGrpc(): com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedReason {
    return when (this) {
        BlockedReason.UNSUBSCRIBED -> com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedReason.UNSUBSCRIBED
        BlockedReason.SPAM_MARKED -> com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedReason.SPAM_MARKED
        BlockedReason.HARD_BOUNCED -> com.sourceforgery.tachikoma.grpc.frontend.blockedemail.BlockedReason.HARD_BOUNCED
    }
}

fun BlockedReason.toGrpcRejectReason(): Rejected.RejectReason {
    return when (this) {
        BlockedReason.UNSUBSCRIBED -> Rejected.RejectReason.UNSUBSCRIBED
        BlockedReason.SPAM_MARKED -> Rejected.RejectReason.SPAM_MARKED
        BlockedReason.HARD_BOUNCED -> Rejected.RejectReason.SPAM_MARKED
    }
}

fun com.sourceforgery.tachikoma.identifiers.IncomingEmailId.toGrpc() =
        IncomingEmailId.newBuilder().setId(incomingEmailId).build()

fun com.sourceforgery.tachikoma.common.NamedEmail.toGrpc() =
        NamedEmailAddress.newBuilder().setEmail(address.address).setName(name).build()

fun AuthenticationRole.toRole() =
        when (this) {
            AuthenticationRole.BACKEND -> AuthRole.BACKEND
            AuthenticationRole.FRONTEND -> AuthRole.FRONTEND
            AuthenticationRole.FRONTEND_ADMIN -> AuthRole.FRONTEND_ADMIN
        }

fun AuthenticationRole.toFrontendRole() =
        when (this) {
            AuthenticationRole.FRONTEND -> FrontendUserRole.FRONTEND
            AuthenticationRole.FRONTEND_ADMIN -> FrontendUserRole.FRONTEND_ADMIN
            else -> throw IllegalArgumentException("$this is not implemented as frontend role")
        }