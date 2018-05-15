package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version3 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
            .createStatement()
            .use {
                it.executeUpdate("ALTER TABLE e_email_status DROP COLUMN mta_status_code")
                it.executeUpdate("ALTER TABLE e_email_status ADD COLUMN meta_data JSON NOT NULL")
            }
        return -3
    }
}