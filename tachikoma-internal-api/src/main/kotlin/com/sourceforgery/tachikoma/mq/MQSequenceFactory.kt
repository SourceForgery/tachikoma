package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain

interface MQSequenceFactory {
    fun listenForDeliveryNotifications(authenticationId: AuthenticationId, callback: (DeliveryNotificationMessage) -> Unit): ListenableFuture<Void>
    fun listenForJobs(callback: (JobMessage) -> Unit): ListenableFuture<Void>
    fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: (T) -> Unit): ListenableFuture<Void>
    fun listenForOutgoingEmails(mailDomain: MailDomain, callback: (OutgoingEmailMessage) -> Unit): ListenableFuture<Void>
    fun listenForIncomingEmails(authenticationId: AuthenticationId, callback: (IncomingEmailNotificationMessage) -> Unit): ListenableFuture<Void>
}
