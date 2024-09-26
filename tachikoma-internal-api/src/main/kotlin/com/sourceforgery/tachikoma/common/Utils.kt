package com.sourceforgery.tachikoma.common

import com.google.protobuf.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

inline fun <T : AutoCloseable, R> T.use(
    closeable: T,
    block: (T) -> R,
): R {
    try {
        return block(closeable)
    } finally {
        closeable.close()
    }
}

inline fun delay(
    millis: Long,
    alwaysRun: () -> Unit,
) {
    try {
        Thread.sleep(millis)
    } finally {
        alwaysRun()
    }
}

inline fun randomDelay(
    millis: LongRange,
    alwaysRun: () -> Unit,
) {
    try {
        val randomMillis = ThreadLocalRandom.current().nextLong(millis.start, millis.endInclusive)
        Thread.sleep(randomMillis)
    } finally {
        alwaysRun()
    }
}

fun Instant.toTimestamp() =
    Timestamp.newBuilder()
        .setSeconds(this.epochSecond)
        .setNanos(this.nano)
        .build()

fun Timestamp.toInstant() = Instant.ofEpochSecond(this.seconds, this.nanos.toLong())

fun Clock.timestamp() = this.instant().toTimestamp()

fun Timestamp.before(other: Timestamp) =
    if (this.seconds == other.seconds) {
        this.nanos < other.nanos
    } else {
        this.seconds < other.seconds
    }
