package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version2 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.execute("ALTER TABLE e_user ADD COLUMN username varchar(255)")
                }
        return -2
    }
}