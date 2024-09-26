package com.sourceforgery.tachikoma

import com.sourceforgery.tachikoma.database.upgrades.DatabaseUpgrade
import io.ebean.ddlrunner.DdlRunner
import java.sql.Connection

class H2DatabaseUpgrade : DatabaseUpgrade {
    override val newVersion: Int = -1

    override fun run(connection: Connection): Int {
        connection.createStatement().use {
            it.executeUpdate("CREATE TABLE IF NOT EXISTS database_version (version INTEGER NOT NULL)")
        }
        val contents =
            requireNotNull(javaClass.getResourceAsStream("/tachikoma-h2-create-all.sql")).use {
                it.reader().readText()
            }
        DdlRunner(false, "h2 upgrade")
            .runAll(contents, connection)
        return newVersion
    }
}
