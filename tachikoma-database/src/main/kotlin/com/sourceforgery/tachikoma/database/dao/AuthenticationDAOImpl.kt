package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.database.objects.query.QAuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
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

    override fun getById(authenticationId: AuthenticationId) =
            ebeanServer.find(AuthenticationDBO::class.java, authenticationId.authenticationId)!!

    override fun getActiveById(authenticationId: AuthenticationId) =
            ebeanServer.find(AuthenticationDBO::class.java)
                    .where()
                    .eq("dbId", authenticationId.authenticationId)
                    .eq("active", true)
                    .findOne()!!
}
