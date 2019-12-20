package com.sourceforgery.tachikoma.database.server

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import javax.annotation.PreDestroy
import javax.inject.Inject
import org.apache.logging.log4j.kotlin.logger

class LogEverything
@Inject
private constructor() : InvokeCounter {

    var slowThreshold = Duration.ofSeconds(1)!!
    private val mapper by lazy(LazyThreadSafetyMode.NONE) {
        ObjectMapper()
    }

    private val loggedQueries = object : LinkedHashMap<String, Long>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > 200
        }
    }

    @PreDestroy
    fun dump() {
        if (LOGGER.delegate.isWarnEnabled) {
            val totalMilliseconds = loggedQueries.asSequence().sumBy { it.value.toInt() }
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
        val LOGGER = logger("sql.log_everything")
    }
}
