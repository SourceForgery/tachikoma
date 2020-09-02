package com.sourceforgery.tachikoma.database.hooks

import io.ebean.Database

interface EbeanHook {
    fun postStart(database: Database)
}
