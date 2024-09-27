package com.sourceforgery.tachikoma.database.objects

import java.time.Instant

sealed class TrackedFilterQuery

data class ClickedBetween(
    val after: Instant,
    val before: Instant,
) : TrackedFilterQuery()

data class OpenedBetween(
    val after: Instant,
    val before: Instant,
) : TrackedFilterQuery()
