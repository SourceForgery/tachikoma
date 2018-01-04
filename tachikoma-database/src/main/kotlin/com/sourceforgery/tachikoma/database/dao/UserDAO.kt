package com.sourceforgery.tachikoma.database.dao

import com.sourceforgery.tachikoma.database.objects.UserDBO
import io.ebean.EbeanServer
import javax.inject.Inject

class UserDAO
@Inject
private constructor(
        private val ebeanServer: EbeanServer
) {
    fun validateApiToken(apiToken: String) =
            ebeanServer.find(UserDBO::class.java)
                    .where()
                    .eq("apiToken", apiToken)
                    .findOne()
}
