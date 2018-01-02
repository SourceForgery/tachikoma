package com.sourceforgery.tachikoma.mq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import com.sourceforgery.tachikoma.common.AccountId
import com.sourceforgery.tachikoma.common.UserId
import com.sourceforgery.tachikoma.common.delay
import com.sourceforgery.tachikoma.common.timestamp
import com.sourceforgery.tachikoma.common.toInstant
import com.sourceforgery.tachikoma.common.toTimestamp
import com.sourceforgery.tachikoma.logging.logger
import java.io.Closeable
import java.time.Clock
import java.time.Duration
import javax.annotation.PreDestroy
import javax.inject.Inject

class ConsumerFactoryImpl
@Inject
private constructor(
        mqConfig: MqConfig,
        val clock: Clock
) : MQSequenceFactory, MQSender {
    @Volatile
    var thread = 0
    val connection: Connection
    val sendChannel: Channel

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

    private class WorkerThread
    internal constructor(
            runnable: Runnable,
            private val threadName: String
    ) : Thread(runnable, threadName) {

        @Synchronized override fun start() {
            if (threadName == name) {
                LOGGER.info { "Starting thread: " + name }
            }
            super.start()
        }
    }

    private fun createQueue(messageQueue: MessageQueue) {
        connection
                .createChannel()
                .use { channel ->
                    val arguments = HashMap<String, Any>()
                    messageQueue.maxLength
                            ?.let { arguments["x-max-length"] = it }

                    if (messageQueue.delay > Duration.ZERO) {
                        arguments["x-message-ttl"] = messageQueue.delay.toMillis().toString()
                    }
                    messageQueue.nextDestination
                            ?.let {
                                arguments["x-dead-letter-routing-key"] = it
                                arguments["x-dead-letter-exchangeType"] = ""
                                arguments["x-dead-letter-exchange"] = ""
                            }

                    channel.queueDeclare(messageQueue.name, true, false, false, arguments)
                }
    }

    private fun createExchange(messageExchange: MessageExchange) {
        connection
                .createChannel()
                .use { channel ->
                    channel.exchangeDeclare(messageExchange.name, messageExchange.exchangeType, true)
                }
    }

    override fun listen(userId: UserId, callback: (NotificationMessage) -> Unit): Closeable {
        val channel = connection
                .createChannel()!!
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray) {
                var worked = false
                try {
                    callback(NotificationMessage.parseFrom(body))
                    channel.basicAck(envelope.deliveryTag, false)
                    worked = true
                } finally {
                    if (!worked) {
                        delay(500, {
                            channel.basicNack(envelope.deliveryTag, false, true)
                        })
                    }
                }
            }
        }
        channel.basicConsume("user.${userId.userId}", false, consumer)
        return Closeable {
            channel.close()
        }
    }

    override fun listenForJobs(callback: (JobMessage) -> Unit): Closeable {
        val channel = connection
                .createChannel()!!
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: ByteArray) {
                val jobMessage = JobMessage.parseFrom(body)!!
                val messageQueue = getRequeueQueue(jobMessage)
                if (messageQueue == null) {
                    // Message has waited long enough
                    var worked = false
                    try {
                        callback(jobMessage)
                        channel.basicAck(envelope.deliveryTag, false)
                        worked = true
                    } finally {
                        if (!worked) {
                            delay(500, {
                                channel.basicNack(envelope.deliveryTag, false, true)
                            })
                        }
                    }
                } else {
                    queueJob(jobMessage)
                    channel.basicAck(envelope.deliveryTag, false)
                }
            }
        }
        channel.basicConsume(JobMessageQueue.JOBS.name, false, consumer)
        return Closeable {
            channel.close()
        }
    }

    private fun getRequeueQueue(jobMessage: JobMessage): JobMessageQueue? {
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
        val queue = getRequeueQueue(jobMessage) ?: JobMessageQueue.JOBS
        val jobMessageClone = JobMessage.newBuilder(jobMessage)
                .setCreationTimestamp(clock.timestamp())
                .build()
        queueJob(jobMessageClone, queue)
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

    override fun queueNotification(accountId: AccountId, notificationMessage: NotificationMessage) {
        val notificationMessageClone = NotificationMessage.newBuilder(notificationMessage)
                .setCreationTimestamp(clock.instant().toTimestamp())
                .build()
        val basicProperties = MessageProperties.MINIMAL_PERSISTENT_BASIC
        sendChannel.basicPublish(
                MessageExchange.EMAIL_NOTIFICATIONS.name,
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