package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.AuthenticationDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class AuthenticationDAOImpl
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) : AuthenticationDAO {
    override fun validateApiToken(apiToken: String) =
            ebeanServer.find(AuthenticationDBO::class.java)
                    .where()
                    .eq("apiToken", apiToken)
                    .findOne()
}
