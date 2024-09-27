package com.sourceforgery.tachikoma.common

import io.ebean.annotation.DbEnumType
import io.ebean.annotation.DbEnumValue

enum class BlockedReason(
    @get:DbEnumValue(storage = DbEnumType.INTEGER)
    @Suppress("unused")
    internal val dbValue: Int,
) {
    UNSUBSCRIBED(0),
    SPAM_MARKED(1),
    HARD_BOUNCED(2),
}
