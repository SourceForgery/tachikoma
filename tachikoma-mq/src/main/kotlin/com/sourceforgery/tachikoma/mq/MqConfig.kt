package com.sourceforgery.tachikoma.mq

import java.net.URI

interface MqConfig {
    val mqUrl: URI
}