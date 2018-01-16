package com.sourceforgery.tachikoma.mq

import com.rabbitmq.client.BuiltinExchangeType

enum class MessageExchange(val exchangeType: BuiltinExchangeType) {
    EMAIL_NOTIFICATIONS(BuiltinExchangeType.TOPIC),
    INCOMING_EMAILS_NOTIFICATIONS(BuiltinExchangeType.TOPIC),
}