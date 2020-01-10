package com.sourceforgery.tachikoma.common

import io.ebean.annotation.DbEnumType
import io.ebean.annotation.DbEnumValue

enum class EmailStatus(
    @Suppress("unused")
    @get:DbEnumValue(storage = DbEnumType.INTEGER)
    val dbValue: Int
) {
    HARD_BOUNCED(0),
    QUEUED(1),
    SOFT_BOUNCED(2),
    DELIVERED(3),
    OPENED(4),
    CLICKED(5),
    SPAM(6),
    UNSUBSCRIBE(7);
}