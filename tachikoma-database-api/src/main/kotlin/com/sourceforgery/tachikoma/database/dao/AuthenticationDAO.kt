package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO

interface AuthenticationDAO {
    fun validateApiToken(apiToken: String): AuthenticationDBO?
}