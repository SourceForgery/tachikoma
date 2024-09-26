package com.sourceforgery.tachikoma.database.objects

import java.time.Instant

sealed class EmailSearchFilterQuery

data class SubjectContains(val subject: String) : EmailSearchFilterQuery()

data class SenderNameContains(val name: String) : EmailSearchFilterQuery()

data class SenderEmailContains(val email: String) : EmailSearchFilterQuery()

data class ReceiverNameContains(val name: String) : EmailSearchFilterQuery()

data class ReceiverEmailContains(val email: String) : EmailSearchFilterQuery()

data class ReceivedBetween(
    val after: Instant,
    val before: Instant,
) : EmailSearchFilterQuery()
