package com.sourceforgery.tachikoma.database.server

interface InvokeCounter {
    fun inc(sql: String?, millis: Long)
}