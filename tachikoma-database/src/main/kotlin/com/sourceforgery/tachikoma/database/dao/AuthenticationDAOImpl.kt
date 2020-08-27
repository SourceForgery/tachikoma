package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import com.sourceforgery.tachikoma.identifiers.AuthenticationId
import io.ebean.Database
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class AuthenticationDAOImpl(override val di: DI) : AuthenticationDAO, DIAware {

    private val database: Database by instance()

    override fun save(authenticationDBO: AuthenticationDBO) =
        database.save(authenticationDBO)

    override fun getByUsername(username: String) =
        database
            .find(AuthenticationDBO::class.java)
            .where()
            .eq("login", username)
            .findOne()

    override fun validateApiToken(apiToken: String): AuthenticationDBO? {
        return database.find(AuthenticationDBO::class.java)
            .where()
            .eq("apiToken", apiToken)
            .findOne()
    }

    override fun getById(authenticationId: AuthenticationId) =
        database.find(AuthenticationDBO::class.java, authenticationId.authenticationId)!!

    override fun getActiveById(authenticationId: AuthenticationId) =
        database.find(AuthenticationDBO::class.java)
            .where()
            .eq("dbId", authenticationId.authenticationId)
            .eq("active", true)
            .findOne()!!

    override fun deleteById(authenticationId: AuthenticationId) {
        database.delete(AuthenticationDBO::class.java, authenticationId.authenticationId)
    }
}
