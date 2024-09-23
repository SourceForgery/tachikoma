package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.common.ExtractEmailMetadata
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.coroutines.TachikomaScopeImpl
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton
import java.time.Clock

val commonModule =
    DI.Module("common") {
        bind<Clock>() with singleton { Clocker() }
        bind<JobMessageFactory>() with singleton { JobMessageFactory(di) }
        bind<TachikomaScope>() with singleton { TachikomaScopeImpl(di) }
        bind<ExtractEmailMetadata>() with singleton { ExtractEmailMetadata(di) }
    }
