package com.sourceforgery.tachikoma.mq

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.impl.ChannelN
import com.sourceforgery.tachikoma.common.calculateMd5
import com.sourceforgery.tachikoma.common.timestamp
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.identifiers.AccountId
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.time.Clock
import java.time.Duration

internal class ConsumerFactoryImpl(override val di: DI) : MQSequenceFactory, MQSender, MQManager, DIAware {
    private val mqConfig: MqConfig by instance()
    private val clock: Clock by instance()

    @Volatile
    private var thread = 0
    private val connection: Connection
    private val sendChannel: Channel

    init {
        val connectionFactory = ConnectionFactory()
        connectionFactory.setThreadFactory { runnable -> WorkerThread(runnable, ConsumerFactoryImpl::class.java.simpleName + " #" + ++thread) }
        connectionFactory.isAutomaticRecoveryEnabled = true
        connectionFactory.setUri(mqConfig.mqUrl)
        connection = connectionFactory.newConnection()
        sendChannel = connection.createChannel()

        createQueue(FAILED_INCOMING_EMAIL_NOTIFICATIONS)
        createQueue(FAILED_DELIVERY_NOTIFICATIONS)
        createQueue(FAILED_OUTGOING_EMAILS)

        for (messageQueue in JobMessageQueue.entries) {
            createQueue(messageQueue)
        }
        for (messageExchange in MessageExchange.entries) {
            createExchange(messageExchange)
        }
    }

    fun close() {
        sendChannel.close()
        connection.close()
    }

    private inner class WorkerThread(
        runnable: Runnable,
        private val threadName: String,
    ) : Thread(runnable, threadName) {
        @Synchronized
        override fun start() {
            if (threadName == name) {
                LOGGER.info { "Starting thread: $name" }
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

    override fun setupAuthentication(
        mailDomain: MailDomain,
        authenticationId: AuthenticationId,
        accountId: AccountId,
    ) {
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

    override fun <T> listenOnQueue(
        messageQueue: MessageQueue<T>,
        callback: suspend (T) -> Unit,
    ): ListenableFuture<Unit> {
        val channel =
            connection
                .createChannel()!!

        val future = SettableFuture.create<Unit>()
        future.addListener(
            {
                LOGGER.info("Closing channel")
                try {
                    channel.close()
                } catch (ignored: Exception) {
                    // 'tis ok
                }
            },
            MoreExecutors.directExecutor(),
        )
        val consumer =
            CallbackConsumer(channel) { payload, version ->
                runBlocking {
                    callback(
                        when (version) {
                            1 -> messageQueue.parser(payload)
                            2 -> messageQueue.parserV2(payload)
                            else -> error("Unknown version $version of payload")
                        },
                    )
                }
            }
        channel.basicConsume(messageQueue.name, false, consumer)
        channel.addShutdownListener {
            if (it.isInitiatedByApplication) {
                future.cancel(false)
            } else {
                (channel as ChannelN).processShutdownSignal(it, true, true)
            }
        }
        return future
    }

    override fun listenForDeliveryAndBlockNotifications(
        authenticationId: AuthenticationId,
        mailDomain: MailDomain,
        accountId: AccountId,
    ): Flow<EmailNotificationEvent> {
        val queue = DeliveryNotificationMessageQueue(authenticationId)
        setupAuthentication(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
        )
        return listenOnQueueFlow(queue)
    }

    override fun listenForOutgoingEmails(mailDomain: MailDomain): Flow<OutgoingEmailMessage> {
        setupAccount(mailDomain)
        return listenOnQueueFlow(OutgoingEmailsMessageQueue(mailDomain))
    }

    override fun listenForIncomingEmails(
        authenticationId: AuthenticationId,
        mailDomain: MailDomain,
        accountId: AccountId,
    ): Flow<IncomingEmailNotificationMessage> {
        setupAuthentication(
            authenticationId = authenticationId,
            mailDomain = mailDomain,
            accountId = accountId,
        )
        return listenOnQueueFlow(IncomingEmailNotificationMessageQueue(authenticationId))
    }

    private fun <T> listenOnQueueFlow(messageQueue: MessageQueue<T>): Flow<T> {
        return callbackFlow {
            val channel =
                withContext(Dispatchers.IO) {
                    connection
                        .createChannel()
                }
            val consumer =
                CallbackConsumer(channel) { payload, version ->
                    runBlocking {
                        val parsed: T =
                            when (version) {
                                1 -> messageQueue.parser(payload)
                                2 -> messageQueue.parserV2(payload)
                                else -> error("Unknown version $version of payload")
                            }
                        send(parsed)
                    }
                }
            channel.basicConsume(messageQueue.name, false, consumer)
            channel.addShutdownListener {
                if (it.isInitiatedByApplication) {
                    this@callbackFlow.cancel()
                } else {
                    (channel as ChannelN).processShutdownSignal(it, true, true)
                }
            }
            awaitClose {
                LOGGER.info { "Closing flow for ${messageQueue.name}" }
                channel.close()
            }
        }
    }

    override fun listenForJobs(callback: suspend (JobMessage) -> Unit): ListenableFuture<Unit> {
        return listenOnQueue(JobMessageQueue.JOBS) {
            val messageQueue = getRequeueQueueByRequestedExecutionTime(it)
            if (messageQueue == null) {
                // Message has waited long enough
                callback(it)
            } else {
                queueJob(it)
            }
        }
    }

    private fun getRequeueQueueByRequestedExecutionTime(jobMessage: JobMessage): JobMessageQueue? {
        val expectedDelay =
            Duration.between(
                clock.instant(),
                jobMessage.requestedExecutionTime.toInstant(),
            )!!
        return SORTED_DELAYED_JOB_QUEUE
            .firstOrNull {
                it.delay <= expectedDelay
            }
    }

    override fun queueJob(jobMessage: JobMessage) {
        val queue = getRequeueQueueByRequestedExecutionTime(jobMessage) ?: JobMessageQueue.JOBS
        val jobMessageClone =
            JobMessage.newBuilder(jobMessage)
                .setCreationTimestamp(clock.timestamp())
                .build()
        queueJob(jobMessageClone, queue)
    }

    override fun queueOutgoingEmail(
        mailDomain: MailDomain,
        outgoingEmailMessage: OutgoingEmailMessage,
    ) {
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
            "",
            OutgoingEmailsMessageQueue(mailDomain).name,
            true,
            basicProperties,
            outgoingEmailMessage.toByteArray(),
        )
    }

    private fun queueJob(
        jobMessage: JobMessage,
        jobMessageQueue: JobMessageQueue,
    ) {
        val basicPropertiesBuilder = basicProperties.builder()
        if (jobMessageQueue.delay > Duration.ZERO) {
            basicPropertiesBuilder.expiration(jobMessageQueue.delay.toMillis().toString())
        }
        sendChannel.basicPublish(
            "",
            jobMessageQueue.name,
            true,
            basicPropertiesBuilder.build(),
            jobMessage.toByteArray(),
        )
    }

    override fun queueDeliveryNotification(
        accountId: AccountId,
        notificationMessage: DeliveryNotificationMessage,
    ) {
        val emailNotificationEvent =
            (EmailNotificationEvent.newBuilder()) {
                creationTimestamp = clock.instant().toTimestamp()
                deliveryNotification = notificationMessage
            }.build()
        sendChannel.basicPublish(
            MessageExchange.DELIVERY_NOTIFICATIONS.name,
            "/account/$accountId",
            true,
            basicProperties,
            emailNotificationEvent.toByteArray(),
        )
    }

    override fun queueEmailBlockingNotification(
        accountId: AccountId,
        emailBlockedMessage: BlockedEmailAddressEvent,
    ) {
        val emailNotificationEvent =
            (EmailNotificationEvent.newBuilder()) {
                creationTimestamp = clock.instant().toTimestamp()
                blockedEmailAddress = emailBlockedMessage
            }.build()
        sendChannel.basicPublish(
            MessageExchange.DELIVERY_NOTIFICATIONS.name,
            "/account/$accountId",
            true,
            basicProperties,
            emailNotificationEvent.toByteArray(),
        )
    }

    override fun queueIncomingEmailNotification(
        accountId: AccountId,
        incomingEmailNotificationMessage: IncomingEmailNotificationMessage,
    ) {
        val notificationMessageClone =
            IncomingEmailNotificationMessage.newBuilder(incomingEmailNotificationMessage)
                .setCreationTimestamp(clock.instant().toTimestamp())
                .build()
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
            MessageExchange.INCOMING_EMAILS_NOTIFICATIONS.name,
            "/account/$accountId",
            true,
            basicProperties,
            notificationMessageClone.toByteArray(),
        )
    }

    private inner class CallbackConsumer(
        channel: Channel,
        private val callback: (payload: ByteArray, version: Int) -> Unit,
    ) : DefaultConsumer(channel) {
        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray,
        ) {
            LOGGER.debug {
                val md5 = calculateMd5(body)
                "Processing message, routing key: ${envelope.routingKey}, consumer tag: $consumerTag, body md5: $md5"
            }
            try {
                callback(body, properties.type?.toIntOrNull() ?: 1)
                LOGGER.debug { "Acked ${envelope.routingKey}" }
                channel.basicAck(envelope.deliveryTag, false)
            } catch (e: Exception) {
                LOGGER.error(e) { "Got exception, consumer tag: $consumerTag" }
                channel.basicNack(envelope.deliveryTag, false, false)
            }
        }
    }

    override fun alive() = connection.isOpen

    companion object {
        val LOGGER = logger()

        val SORTED_DELAYED_JOB_QUEUE =
            JobMessageQueue.entries
                .asSequence()
                .filter { it.nextDestination == JobMessageQueue.JOBS }
                .sortedByDescending { it.delay }
                .toList()

        private val basicProperties =
            MessageProperties.MINIMAL_PERSISTENT_BASIC
                .builder()
                .type("2")
                .build()
    }
}
