package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.database.objects.EmailDAO
import org.glassfish.hk2.utilities.binding.AbstractBinder
import javax.inject.Singleton

class DatabaseBinder : AbstractBinder() {
    override fun configure() {
        bind(EmailDAO::class.java)
                .`in`(Singleton::class.java)
    }
}