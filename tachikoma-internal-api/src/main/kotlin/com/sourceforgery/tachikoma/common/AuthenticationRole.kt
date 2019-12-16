package com.sourceforgery.tachikoma.common

import io.ebean.annotation.DbEnumValue

enum class AuthenticationRole(
    @get:DbEnumValue
    val stableValue: Int
) {
    BACKEND(0),
    FRONTEND(1),
    FRONTEND_ADMIN(2);
}