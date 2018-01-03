package com.sourceforgery.tachikoma.webserver

import com.linecorp.armeria.common.HttpRequest
import com.sourceforgery.tachikoma.auth.JwtAuth
import com.sourceforgery.tachikoma.identifiers.UserId
import org.glassfish.hk2.api.Factory
import javax.inject.Inject

class JwtAuthFactory
@Inject
private constructor(
        private val httpRequest: HttpRequest
) : Factory<JwtAuth> {
    override fun provide(): JwtAuth {
        val headers = httpRequest.headers()!!
        return object : JwtAuth {
            override val userId: UserId
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        }
    }

    override fun dispose(instance: JwtAuth?) {
    }

}