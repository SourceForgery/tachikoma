package com.sourceforgery.tachikoma.logging

interface InvokeCounter {
    fun inc(sql: String?, millis: Long)
    fun dump()
}

interface InvokeCounterFactory {
    fun create(): InvokeCounter
}
