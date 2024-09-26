package com.sourceforgery.tachikoma.logging

interface InvokeCounter {
    fun inc(
        sql: String?,
        millis: Long,
    )
}

interface InvokeCounterFactory {
    fun create(): InvokeCounter
}
