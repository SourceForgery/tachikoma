package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.AuthenticationId

interface AuthenticationDAO {
    fun validateApiToken(apiToken: String): AuthenticationDBO?
    fun getById(authenticationId: AuthenticationId): AuthenticationDBO?
    fun getActiveById(authenticationId: AuthenticationId): AuthenticationDBO?
    fun getByUsername(username: String): AuthenticationDBO?
}