package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import com.sourceforgery.tachikoma.common.timestamp
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.hk2.HK2RequestContext
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.logging.logger
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executors
import javax.annotation.PreDestroy
import javax.inject.Inject

internal class ConsumerFactoryImpl
@Inject
private constructor(
        mqConfig: MqConfig,
        private val clock: Clock,
        private val hK2RequestContext: HK2RequestContext
) : MQSequenceFactory, MQSender, MQManager {
    @Volatile
    private var thread = 0
    private val connection: Connection
    private val sendChannel: Channel
    private val closeExecutor = Executors.newSingleThreadExecutor()!!

    init {
        val connectionFactory = ConnectionFactory()
        connectionFactory.setThreadFactory { runnable -> WorkerThread(runnable, ConsumerFactoryImpl::class.java.simpleName + " #" + ++thread) }
        connectionFactory.isAutomaticRecoveryEnabled = true
        connectionFactory.setUri(mqConfig.mqUrl)
        connection = connectionFactory.newConnection()
        sendChannel = connection.createChannel()

        for (messageQueue in JobMessageQueue.values()) {
            createQueue(messageQueue)
        }
        for (messageExchange in MessageExchange.values()) {
            createExchange(messageExchange)
        }
    }

    @PreDestroy
    private fun close() {
        sendChannel.close()
        connection.close()
    }

    private inner class WorkerThread
    internal constructor(
            runnable: Runnable,
            private val threadName: String
    ) : Thread(runnable, threadName) {

        @Synchronized
        override fun start() {
            if (threadName == name) {
                LOGGER.info { "Starting thread: " + name }
            }
            super.start()
        }
    }

    override fun setupAccount(mailDomain: MailDomain) {
        createQueue(OutgoingEmailsMessageQueue(mailDomain))
    }

    override fun removeAccount(mailDomain: MailDomain) {
        removeQueue(OutgoingEmailsMessageQueue(mailDomain))
    }

    override fun setupAuthentication(mailDomain: MailDomain, authenticationId: AuthenticationId, accountId: AccountId) {
        val incomingEmailQueue = IncomingEmailNotificationMessageQueue(authenticationId = authenticationId)
        createQueue(incomingEmailQueue)
        sendChannel.queueBind(incomingEmailQueue.name, MessageExchange.INCOMING_EMAILS_NOTIFICATIONS.name, "/account/$accountId")

        val deliveryNotifications = DeliveryNotificationMessageQueue(authenticationId = authenticationId)
        createQueue(deliveryNotifications)
        sendChannel.queueBind(deliveryNotifications.name, MessageExchange.DELIVERY_NOTIFICATIONS.name, "/account/$accountId")
    }

    override fun removeAuthentication(authenticationId: AuthenticationId) {
        removeQueue(IncomingEmailNotificationMessageQueue(authenticationId = authenticationId))
        removeQueue(DeliveryNotificationMessageQueue(authenticationId = authenticationId))
    }

    private fun createQueue(messageQueue: MessageQueue<*>) {
        val arguments = HashMap<String, Any>()
        messageQueue.maxLength
                ?.let { arguments["x-max-length"] = it }

        if (messageQueue.delay > Duration.ZERO) {
            arguments["x-message-ttl"] = messageQueue.delay.toMillis()
        }
        messageQueue.nextDestination
                ?.let {
                    arguments["x-dead-letter-routing-key"] = it.name
                    arguments["x-dead-letter-exchangeType"] = ""
                    arguments["x-dead-letter-exchange"] = ""
                }

        sendChannel.queueDeclare(messageQueue.name, true, false, false, arguments)
    }

    private fun removeQueue(messageQueue: MessageQueue<*>) {
        sendChannel.queueDelete(messageQueue.name)
    }

    private fun createExchange(messageExchange: MessageExchange) {
        connection
                .createChannel()
                .use { channel ->
                    channel.exchangeDeclare(messageExchange.name, messageExchange.exchangeType, true)
                }
    }

    override fun <T> listenOnQueue(messageQueue: MessageQueue<T>, callback: (T) -> Unit): ListenableFuture<Void> {
        val ctx = hK2RequestContext.getContextInstance()
        val channel = connection
                .createChannel()!!

        val future = SettableFuture.create<Void>()
        future.addListener(Runnable { channel.close() }, closeExecutor)
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray) {
                var handledResult = false
                try {
                    val parsedMessage = messageQueue.parser(body)
                    hK2RequestContext.runInScope(ctx, { callback(parsedMessage) })
                    channel.basicAck(envelope.deliveryTag, false)
                    handledResult = true
                } catch (e: Exception) {
                    future.setException(e)
                    handledResult = true
                } finally {
                    if (!handledResult) {
                        // Will be NACK'd by closing channel when Future completes
                        future.cancel(true)
                    }
                }
            }
        }
        channel.basicConsume(messageQueue.name, false, consumer)
        return future
    }

    override fun listenForDeliveryNotifications(authenticationId: AuthenticationId, callback: (DeliveryNotificationMessage) -> Unit): ListenableFuture<Void> {
        val queue = DeliveryNotificationMessageQueue(authenticationId = authenticationId)
        return listenOnQueue(queue, callback)
    }

    override fun listenForOutgoingEmails(mailDomain: MailDomain, callback: (OutgoingEmailMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(OutgoingEmailsMessageQueue(mailDomain), callback)
    }

    override fun listenForJobs(callback: (JobMessage) -> Unit): ListenableFuture<Void> {
        return listenOnQueue(JobMessageQueue.JOBS, { it ->
            val messageQueue = getRequeueQueueByRequestedExecutionTime(it)
            if (messageQueue == null) {
                // Message has waited long enough
                callback(it)
            } else {
                queueJob(it)
            }
        })
    }

    private fun getRequeueQueueByRequestedExecutionTime(jobMessage: JobMessage): JobMessageQueue? {
        val expectedDelay = Duration.between(
                clock.instant(),
                jobMessage.requestedExecutionTime.toInstant()
        )!!
        return SORTED_DELAYED_JOB_QUEUE
                .firstOrNull {
                    it.delay <= expectedDelay
                }
    }

    override fun queueJob(jobMessage: JobMessage) {
        val queue = getRequeueQueueByRequestedExecutionTime(jobMessage) ?: JobMessageQueue.JOBS
        val jobMessageClone = JobMessage.newBuilder(jobMessage)
                .setCreationTimestamp(clock.timestamp())
                .build()
        queueJob(jobMessageClone, queue)
    }

    override fun queueOutgoingEmail(mailDomain: MailDomain, outgoingEmailMessage: OutgoingEmailMessage) {
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
                "",
                OutgoingEmailsMessageQueue(mailDomain).name,
                true,
                basicProperties,
                outgoingEmailMessage.toByteArray()
        )
    }

    private fun queueJob(jobMessage: JobMessage, jobMessageQueue: JobMessageQueue) {
        var basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        if (jobMessageQueue.delay > Duration.ZERO) {
            basicProperties = basicProperties.builder()
                    .expiration(jobMessageQueue.delay.toMillis().toString())
                    .build()
        }
        sendChannel.basicPublish(
                "",
                jobMessageQueue.name,
                true,
                basicProperties,
                jobMessage.toByteArray()
        )
    }

    override fun queueDeliveryNotification(accountId: AccountId, notificationMessage: DeliveryNotificationMessage) {
        val notificationMessageClone = DeliveryNotificationMessage.newBuilder(notificationMessage)
                .setCreationTimestamp(clock.instant().toTimestamp())
                .build()
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
                MessageExchange.DELIVERY_NOTIFICATIONS.name,
                "/account/$accountId",
                true,
                basicProperties,
                notificationMessageClone.toByteArray()
        )
    }

    override fun queueIncomingEmailNotification(accountId: AccountId, incomingEmailNotificationMessage: IncomingEmailNotificationMessage) {
        val notificationMessageClone = IncomingEmailNotificationMessage.newBuilder(incomingEmailNotificationMessage)
                .setCreationTimestamp(clock.instant().toTimestamp())
                .build()
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
                MessageExchange.INCOMING_EMAILS_NOTIFICATIONS.name,
                "/account/$accountId",
                true,
                basicProperties,
                notificationMessageClone.toByteArray()
        )
    }

    companion object {
        val LOGGER = logger()

        val SORTED_DELAYED_JOB_QUEUE = JobMessageQueue
                .values()
                .asSequence()
                .filterNot { it == JobMessageQueue.JOBS }
                .sortedByDescending { it.delay }
                .toList()
    }
}