package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import kotlinx.coroutines.flow.Flow

interface MQSequenceFactory {
    fun listenForDeliveryNotifications(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId): Flow<DeliveryNotificationMessage>
    fun listenForJobs(callback: suspend (JobMessage) -> Unit): ListenableFuture<Void>
    fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: suspend (T) -> Unit): ListenableFuture<Void>
    fun listenForOutgoingEmails(mailDomain: MailDomain): Flow<OutgoingEmailMessage>
    fun listenForIncomingEmails(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId): Flow<IncomingEmailNotificationMessage>
    fun alive(): Boolean
}
