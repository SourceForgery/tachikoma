package com.sourceforgery.tachikoma.database.hooks

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
                            it.executeUpdate("INSERT INTO database_version VALUES (-1)")
                            return -1
                        }
                    }
                }
    }
}