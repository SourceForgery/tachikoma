package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import org.kodein.di.DI
import org.kodein.di.DIAware

class MQSequenceFactoryMock(override val di: DI) : MQSequenceFactory, DIAware {
    val deliveryNotifications = LinkedBlockingQueue<QueueMessageWrap<DeliveryNotificationMessage>>(1)
    val jobs = LinkedBlockingQueue<QueueMessageWrap<JobMessage>>(1)
    val outgoingEmails = LinkedBlockingQueue<QueueMessageWrap<OutgoingEmailMessage>>(1)
    val incomingEmails = LinkedBlockingQueue<QueueMessageWrap<IncomingEmailNotificationMessage>>(1)

    override fun listenForDeliveryNotifications(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId, callback: (DeliveryNotificationMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(deliveryNotifications, callback)
    }

    private fun <X : Any> listenOnQueue(queue: BlockingQueue<QueueMessageWrap<X>>, callback: (X) -> Unit): SettableFuture<Void> {
        val future = SettableFuture.create<Void>()
        executorService.execute {
            generateSequence {
                queue.take().value
            }.forEach {
                callback(it)
            }
            future.set(null)
        }
        return future
    }

    override fun listenForJobs(callback: (JobMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(jobs, callback)
    }

    override fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: (T) -> Unit): ListenableFuture<Void> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun listenForOutgoingEmails(mailDomain: MailDomain, callback: (OutgoingEmailMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(outgoingEmails, callback)
    }

    override fun listenForIncomingEmails(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId, callback: (IncomingEmailNotificationMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(incomingEmails, callback)
    }

    companion object {
        private val executorService: ExecutorService = Executors.newCachedThreadPool()
    }
}
