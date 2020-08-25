package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version7 : DatabaseUpgrade {
    override val newVersion: Int = -7

    override fun run(connection: Connection): Int {
        connection
            .createStatement()
            .use {
                it.execute("ALTER TABLE e_email ADD COLUMN subject TEXT")
            }
        return -7
    }
}
