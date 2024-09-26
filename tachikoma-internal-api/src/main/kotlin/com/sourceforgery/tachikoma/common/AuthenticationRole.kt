package com.sourceforgery.tachikoma.common

import io.ebean.annotation.DbEnumType
import io.ebean.annotation.DbEnumValue

enum class AuthenticationRole(
    @get:DbEnumValue(storage = DbEnumType.INTEGER)
    val stableValue: Int,
) {
    BACKEND(0),
    FRONTEND(1),
    FRONTEND_ADMIN(2),
}
