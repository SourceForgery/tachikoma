package com.sourceforgery.tachikoma.database.hooks

import io.ebean.migration.ddl.DdlRunner
import java.nio.charset.StandardCharsets
import java.sql.Connection

class CreateDatabaseVersionTable : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.executeUpdate("CREATE TABLE IF NOT EXISTS database_version (version INTEGER NOT NULL)")
                    it.executeQuery("SELECT version FROM database_version").use { resultset ->
                        if (resultset.next()) {
                            return resultset.getInt(1)
                        } else {
                            val content = javaClass.getResourceAsStream("/create-all.sql").use {
                                it.reader(StandardCharsets.UTF_8).readText()
                            }
                            DdlRunner(false, "create-all.sql")
                                    .runAll(content, connection)
                            return -1
                        }
                    }
                }
    }
}