package com.sourceforgery.tachikoma.common

import com.google.protobuf.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

inline fun randomDelay(
    millis: LongRange,
    alwaysRun: () -> Unit,
) {
    try {
        val randomMillis = ThreadLocalRandom.current().nextLong(millis.first, millis.last)
        Thread.sleep(randomMillis)
    } finally {
        alwaysRun()
    }
}

fun Instant.toTimestamp(): Timestamp =
    Timestamp.newBuilder()
        .setSeconds(this.epochSecond)
        .setNanos(this.nano)
        .build()

fun Timestamp.toInstant(): Instant = Instant.ofEpochSecond(this.seconds, this.nanos.toLong())

fun Clock.timestamp() = this.instant().toTimestamp()

fun Timestamp.before(other: Timestamp) =
    if (this.seconds == other.seconds) {
        this.nanos < other.nanos
    } else {
        this.seconds < other.seconds
    }
