package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sourceforgery.tachikoma.logging.InvokeCounter
import org.apache.logging.log4j.kotlin.logger
import org.kodein.di.bindings.ScopeCloseable
import java.time.Duration

class LogEverything : InvokeCounter, ScopeCloseable {

    var slowThreshold = Duration.ofSeconds(1)!!

    private val loggedQueries = object : LinkedHashMap<String, Long>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 200
        }
    }

    fun dump() {
        if (LOGGER.delegate.isWarnEnabled) {
            val totalMilliseconds = loggedQueries.asSequence().sumOf { it.value.toInt() }
            if (totalMilliseconds > slowThreshold.toMillis()) {
                LOGGER.warn { "Slow req: ${mapper.writeValueAsString(loggedQueries)}" }
            }
        }
    }

    override fun inc(sql: String?, millis: Long) {
        sql?.let {
            LOGGER.trace { "${millis}ms for $sql" }
            loggedQueries.compute(it) { _, v -> (v ?: 0) + millis }
        }
    }

    companion object {
        private val mapper = jacksonObjectMapper()
        private val LOGGER = logger("sql.log_everything")
    }

    override fun close() {
        dump()
        loggedQueries.clear()
    }
}

object LogNothing : InvokeCounter {
    override fun inc(sql: String?, millis: Long) {
    }
}
