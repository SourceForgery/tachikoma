package com.sourceforgery.tachikoma.database.server

import io.ebean.config.ServerConfig

interface DataSourceProvider {
    fun provide(serverConfig: ServerConfig)
}