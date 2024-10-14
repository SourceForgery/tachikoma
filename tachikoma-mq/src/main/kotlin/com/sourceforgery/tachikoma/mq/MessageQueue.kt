package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import com.sourceforgery.tachikoma.identifiers.MailDomain
import com.sourceforgery.tachikoma.invoke
import java.time.Duration

enum class JobMessageQueue(
    override val delay: Duration = Duration.ZERO,
    override val maxLength: Int? = null,
    override val nextDestination: MessageQueue<JobMessage>? = null,
) : MessageQueue<JobMessage> {
    FAILED_JOBS,
    JOBS(nextDestination = FAILED_JOBS),
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
    override val parserV2: (ByteArray) -> JobMessage
        get() = parser

    init {
        assert(delay == Duration.ZERO || nextDestination != null)
    }
}

internal val FAILED_OUTGOING_EMAILS =
    object : MessageQueue<OutgoingEmailMessage> {
        override val delay = Duration.ZERO
        override val maxLength = null
        override val nextDestination = null
        override val name = "FAILED_OUTGOING_EMAILS"
        override val parser: (ByteArray) -> OutgoingEmailMessage = {
            error("No parser for this")
        }
        override val parserV2: (ByteArray) -> OutgoingEmailMessage
            get() = parser
    }

class OutgoingEmailsMessageQueue(
    mailDomain: MailDomain,
) : MessageQueue<OutgoingEmailMessage> {
    override val name = "outgoing.$mailDomain"
    override val maxLength = null
    override val delay = Duration.ZERO
    override val nextDestination = FAILED_OUTGOING_EMAILS
    override val parser: (ByteArray) -> OutgoingEmailMessage = OutgoingEmailMessage::parseFrom
    override val parserV2: (ByteArray) -> OutgoingEmailMessage
        get() = parser
}

internal val FAILED_DELIVERY_NOTIFICATIONS =
    object : MessageQueue<EmailNotificationEvent> {
        override val delay = Duration.ZERO
        override val maxLength = null
        override val nextDestination = null
        override val name = "FAILED_DELIVERY_NOTIFICATIONS"
        override val parser: (ByteArray) -> EmailNotificationEvent = {
            error("No parser for this")
        }
        override val parserV2: (ByteArray) -> EmailNotificationEvent
            get() = parser
    }

class DeliveryNotificationMessageQueue(
    authenticationId: AuthenticationId,
    override val maxLength: Int? = null,
) : MessageQueue<EmailNotificationEvent> {
    override val name = "deliverynotifications.$authenticationId"
    override val delay = Duration.ZERO
    override val nextDestination = FAILED_DELIVERY_NOTIFICATIONS
    override val parser: (ByteArray) -> EmailNotificationEvent
        get() = {
            val notificationMessage = DeliveryNotificationMessage.parseFrom(it)
            (EmailNotificationEvent.newBuilder()) {
                deliveryNotification = notificationMessage
                @Suppress("DEPRECATION")
                creationTimestamp = notificationMessage.creationTimestamp
            }.build()
        }
    override val parserV2: (ByteArray) -> EmailNotificationEvent = EmailNotificationEvent::parseFrom
}

internal val FAILED_INCOMING_EMAIL_NOTIFICATIONS =
    object : MessageQueue<IncomingEmailNotificationMessage> {
        override val delay = Duration.ZERO
        override val maxLength = null
        override val nextDestination = null
        override val name = "FAILED_INCOMING_EMAIL_NOTIFICATIONS"
        override val parser: (ByteArray) -> IncomingEmailNotificationMessage = {
            error("No parser for this")
        }
        override val parserV2: (ByteArray) -> IncomingEmailNotificationMessage
            get() = parser
    }

class IncomingEmailNotificationMessageQueue(
    authenticationId: AuthenticationId,
    override val maxLength: Int? = null,
) : MessageQueue<IncomingEmailNotificationMessage> {
    override val name = "incomingemail.$authenticationId"
    override val delay = Duration.ZERO
    override val nextDestination = FAILED_INCOMING_EMAIL_NOTIFICATIONS
    override val parser: (ByteArray) -> IncomingEmailNotificationMessage = IncomingEmailNotificationMessage::parseFrom
    override val parserV2: (ByteArray) -> IncomingEmailNotificationMessage
        get() = parser
}
