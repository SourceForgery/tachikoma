package com.sourceforgery.tachikoma.database.server

import io.ebean.config.DatabaseConfig

interface DataSourceProvider {
    fun provide(serverConfig: DatabaseConfig)
}
