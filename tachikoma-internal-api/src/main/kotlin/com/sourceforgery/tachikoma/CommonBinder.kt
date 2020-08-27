package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.common.Clocker
import com.sourceforgery.tachikoma.coroutines.TachikomaScope
import com.sourceforgery.tachikoma.coroutines.TachikomaScopeImpl
import com.sourceforgery.tachikoma.mq.JobMessageFactory
import java.time.Clock
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val commonModule = DI.Module("common") {
    bind<Clock>() with singleton { Clocker() }
    bind<JobMessageFactory>() with singleton { JobMessageFactory(di) }
    bind<TachikomaScope>() with singleton { TachikomaScopeImpl(di) }
}
