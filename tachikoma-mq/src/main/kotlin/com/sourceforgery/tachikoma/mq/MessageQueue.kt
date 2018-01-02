package com.sourceforgery.tachikoma.mq

import java.time.Duration

enum class JobMessageQueue(
        override val delay: Duration = Duration.ZERO,
        override val maxLength: Int? = null,
        override val nextDestination: MessageQueue? = null
) : MessageQueue {
    JOBS,
    JOBS_1_MIN(delay = Duration.ofMinutes(1), nextDestination = JOBS),
    JOBS_2_MIN(delay = Duration.ofMinutes(2), nextDestination = JOBS),
    JOBS_4_MIN(delay = Duration.ofMinutes(4), nextDestination = JOBS),
    JOBS_8_MIN(delay = Duration.ofMinutes(8), nextDestination = JOBS),
    JOBS_16_MIN(delay = Duration.ofMinutes(16), nextDestination = JOBS),
    JOBS_32_MIN(delay = Duration.ofMinutes(32), nextDestination = JOBS),
    JOBS_1_HOUR(delay = Duration.ofHours(1), nextDestination = JOBS),
    JOBS_2_HOURS(delay = Duration.ofHours(2), nextDestination = JOBS),
    JOBS_4_HOURS(delay = Duration.ofHours(4), nextDestination = JOBS),
    JOBS_8_HOURS(delay = Duration.ofHours(8), nextDestination = JOBS),
    JOBS_16_HOURS(delay = Duration.ofHours(16), nextDestination = JOBS),
    JOBS_1_DAY(delay = Duration.ofDays(1), nextDestination = JOBS),
    ;

    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}

interface MessageQueue {
    val delay: Duration
    val maxLength: Int?
    val nextDestination: MessageQueue?
    val name: String
}

class MessageQueueImpl(
        override val delay: Duration = Duration.ZERO,
        override val maxLength: Int? = null,
        override val nextDestination: MessageQueue? = null,
        override val name: String
) : MessageQueue {
    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}
