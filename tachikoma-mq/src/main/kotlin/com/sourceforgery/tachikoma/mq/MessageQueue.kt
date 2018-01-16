package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import java.time.Duration

enum class JobMessageQueue(
        override val delay: Duration = Duration.ZERO,
        override val maxLength: Int? = null,
        override val nextDestination: MessageQueue<JobMessage>? = null
) : MessageQueue<JobMessage> {
    JOBS,
    JOBS_30_SEC(delay = Duration.ofSeconds(30), nextDestination = JOBS),
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

    override val parser: (ByteArray) -> JobMessage = JobMessage::parseFrom

    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}

class OutgoingEmailsMessageQueue(
        mailDomain: MailDomain
) : MessageQueue<OutgoingEmailMessage> {
    override val name = "outgoing.$mailDomain"
    override val maxLength: Int? = null
    override val delay: Duration = Duration.ZERO
    override val nextDestination: MessageQueue<OutgoingEmailMessage>? = null
    override val parser: (ByteArray) -> OutgoingEmailMessage = OutgoingEmailMessage::parseFrom

    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}

class DeliveryNotificationMessageQueue(
        override val maxLength: Int? = null,
        authenticationId: AuthenticationId
) : MessageQueue<DeliveryNotificationMessage> {
    override val name = "deliverynotifications.$authenticationId"
    override val delay: Duration = Duration.ZERO
    override val nextDestination: MessageQueue<DeliveryNotificationMessage>? = null
    override val parser: (ByteArray) -> DeliveryNotificationMessage = DeliveryNotificationMessage::parseFrom

    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}

class IncomingEmailNotificationMessageQueue(
        override val maxLength: Int? = null,
        mailDomain: MailDomain,
        authenticationId: AuthenticationId
) : MessageQueue<DeliveryNotificationMessage> {
    override val name = "incomingemail.$authenticationId.$mailDomain"
    override val delay: Duration = Duration.ZERO
    override val nextDestination: MessageQueue<DeliveryNotificationMessage>? = null
    override val parser: (ByteArray) -> DeliveryNotificationMessage = DeliveryNotificationMessage::parseFrom

    init {
        assert((delay == Duration.ZERO) == (nextDestination == null))
    }
}
