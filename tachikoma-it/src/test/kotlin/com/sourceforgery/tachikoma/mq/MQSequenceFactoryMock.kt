package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.DIAware
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

class MQSequenceFactoryMock(override val di: DI) : MQSequenceFactory, DIAware {
    val deliveryNotifications = Channel<DeliveryNotificationMessage>(UNLIMITED)
    val jobs = LinkedBlockingQueue<QueueMessageWrap<JobMessage>>(1)
    val outgoingEmails = Channel<OutgoingEmailMessage>(UNLIMITED)
    val incomingEmails = Channel<IncomingEmailNotificationMessage>(UNLIMITED)

    override fun listenForDeliveryNotifications(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId): Flow<DeliveryNotificationMessage> =
        deliveryNotifications
            .consumeAsFlow()

    private fun <X : Any> listenOnQueue(queue: BlockingQueue<QueueMessageWrap<X>>, callback: suspend (X) -> Unit): SettableFuture<Unit> {
        val future = SettableFuture.create<Unit>()
        executorService.execute {
            generateSequence {
                queue.take().value
            }.forEach {
                runBlocking {
                    callback(it)
                }
            }
            future.set(Unit)
        }
        return future
    }

    override fun listenForJobs(callback: suspend (JobMessage) -> Unit): ListenableFuture<Unit> {
        return listenOnQueue(jobs, callback)
    }

    override fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: suspend (T) -> Unit): ListenableFuture<Unit> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun listenForOutgoingEmails(mailDomain: MailDomain): Flow<OutgoingEmailMessage> {
        return outgoingEmails.consumeAsFlow()
    }

    override fun listenForIncomingEmails(authenticationId: AuthenticationId, mailDomain: MailDomain, accountId: AccountId): Flow<IncomingEmailNotificationMessage> {
        return incomingEmails.consumeAsFlow()
    }

    companion object {
        private val executorService: ExecutorService = Executors.newCachedThreadPool()
    }

    override fun alive() = true
}
