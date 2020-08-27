package com.sourceforgery.tachikoma.startup

import com.sourceforgery.tachikoma.config.Configuration
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val startupModule = DI.Module("startup") {
    bind<Configuration>() with singleton { Configuration() }
}
