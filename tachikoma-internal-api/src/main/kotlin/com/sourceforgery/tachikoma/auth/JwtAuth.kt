package com.sourceforgery.tachikoma.auth

import com.sourceforgery.tachikoma.identifiers.UserId

interface JwtAuth {
    val userId: UserId
}