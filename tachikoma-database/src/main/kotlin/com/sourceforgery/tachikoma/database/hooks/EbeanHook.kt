package com.sourceforgery.tachikoma.database.hooks

import io.ebean.EbeanServer

abstract class EbeanHook {
    open fun postStart(ebeanServer: EbeanServer) {}
}