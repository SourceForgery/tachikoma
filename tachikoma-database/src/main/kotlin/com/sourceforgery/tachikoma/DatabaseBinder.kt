package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.database.objects.EmailDAO
import com.sourceforgery.tachikoma.database.server.EbeanServerFactory
import com.sourceforgery.tachikoma.database.server.InvokeCounter
import com.sourceforgery.tachikoma.database.server.LogEverything
import com.sourceforgery.tachikoma.hk2.RequestScoped
import io.ebean.EbeanServer
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class DatabaseBinder : AbstractBinder() {
    override fun configure() {
        bindFactory(EbeanServerFactory::class.java)
                .to(EbeanServer::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(EmailDAO::class.java)
                .`in`(Singleton::class.java)
        bindAsContract(LogEverything::class.java)
                .to(InvokeCounter::class.java)
                .`in`(RequestScoped::class.java)
    }
}