package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.ebean.EbeanServer
import javax.inject.Inject

class AuthenticationDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : AuthenticationDAO {
    override fun save(authenticationDBO: AuthenticationDBO) =
            ebeanServer.save(authenticationDBO)

    override fun getByUsername(username: String) =
            ebeanServer
                    .find(AuthenticationDBO::class.java)
                    .where()
                    .eq("login", username)
                    .findOne()

    override fun validateApiToken(apiToken: String): AuthenticationDBO? {
        return ebeanServer.find(AuthenticationDBO::class.java)
                .where()
                .eq("apiToken", apiToken)
                .findOne()
    }

    override fun getById(authenticationId: AuthenticationId) =
            ebeanServer.find(AuthenticationDBO::class.java, authenticationId.authenticationId)!!

    override fun getActiveById(authenticationId: AuthenticationId) =
            ebeanServer.find(AuthenticationDBO::class.java)
                    .where()
                    .eq("dbId", authenticationId.authenticationId)
                    .eq("active", true)
                    .findOne()!!

    override fun deleteById(authenticationId: AuthenticationId) {
        ebeanServer.delete(AuthenticationDBO::class.java, authenticationId.authenticationId)
    }
}
