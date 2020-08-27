package com.sourceforgery.tachikoma.mq

import com.sourceforgery.tachikoma.mq.jobs.JobFactory
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val mqModule = DI.Module("mq") {
    bind<ConsumerFactoryImpl>() with singleton { ConsumerFactoryImpl(di) }
    bind<JobFactory>() with singleton { JobFactory(di) }
    bind<JobWorker>() with singleton { JobWorker(di) }
}
