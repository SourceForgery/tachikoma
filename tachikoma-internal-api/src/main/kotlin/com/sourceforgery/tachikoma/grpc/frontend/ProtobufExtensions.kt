package com.sourceforgery.tachikoma.grpc.frontend

import com.sourceforgery.tachikoma.identifiers.EmailId

fun com.sourceforgery.tachikoma.common.Email.toGrpcInternal() =
        Email.newBuilder().setEmail(address).build()

fun Email.toEmail() =
        com.sourceforgery.tachikoma.common.Email(email)

fun grpcEmailInternal(emailAddress: String) =
        Email.newBuilder().setEmail(emailAddress).build()

fun com.sourceforgery.tachikoma.common.NamedEmail.toGrpcInternal() =
        NamedEmail.newBuilder()
                .setEmail(address.address)
                .setName(name)
                .build()

fun NamedEmail.toNamedEmail() =
        com.sourceforgery.tachikoma.common.NamedEmail(com.sourceforgery.tachikoma.common.Email(email), name)

fun EmailId.toGrpcInternal() =
        EmailMessageId.newBuilder().setId(emailId).build()

fun com.sourceforgery.tachikoma.identifiers.EmailTransactionId.toGrpcInternal() =
        EmailTransactionId.newBuilder().setId(emailTransactionId).build()