package com.sourceforgery.tachikoma.rest

import com.sourceforgery.tachikoma.rest.catchers.RestExceptionMap
import com.sourceforgery.tachikoma.rest.tracking.TrackingRest
import com.sourceforgery.tachikoma.rest.unsubscribe.AbuseReportingService
import com.sourceforgery.tachikoma.rest.unsubscribe.UnsubscribeRest
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val restModule =
    DI.Module("rest") {
        bind<TrackingRest>() with singleton { TrackingRest(di) }
        bind<UnsubscribeRest>() with singleton { UnsubscribeRest(di) }
        bind<AbuseReportingService>() with singleton { AbuseReportingService(di) }
        bind<RootPage>() with singleton { RootPage(di) }

        bind<RestExceptionMap>() with singleton { RestExceptionMap(di) }
    }
