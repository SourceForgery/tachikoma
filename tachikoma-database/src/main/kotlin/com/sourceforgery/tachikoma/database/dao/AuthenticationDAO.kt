package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.query.QAuthenticationDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class AuthenticationDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : AuthenticationDAO {
    override fun validateApiToken(apiToken: String): AuthenticationDBO? {
        val query = QAuthenticationDBO(ebeanServer)
        query.apiToken.eq(apiToken)
        return query.findOne()
    }
}
