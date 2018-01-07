package com.sourceforgery.tachikoma.database.hooks

import com.sourceforgery.tachikoma.config.DatabaseConfig
import io.ebean.EbeanServer
import javax.inject.Inject

class CreateSequence
@Inject
private constructor(
        private val databaseConfig: DatabaseConfig
) : EbeanHook() {
    override fun postStart(ebeanServer: EbeanServer) {
        if (databaseConfig.wipeAndCreateDatabase) {
            ebeanServer.createSqlUpdate("CREATE SEQUENCE IF NOT EXISTS unique_id_seq;").execute()
        }
    }
}