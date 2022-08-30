package com.sourceforgery.tachikoma

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun <T> ProducerScope<T>.withKeepAlive(keepAlive: T) {
    launch {
        try {
            while (true) {
                delay(10_000L)
                send(keepAlive)
            }
        } catch (_: Exception) {
        }
    }
}
