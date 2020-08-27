package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version6 : DatabaseUpgrade {
    override val newVersion: Int = -6

    override fun run(connection: Connection): Int {
        connection
            .createStatement()
            .use {
                it.execute("ALTER TABLE e_email ADD COLUMN meta_data JSON NOT NULL DEFAULT '{}'::JSON")
                it.execute("ALTER TABLE e_email_send_transaction ADD COLUMN meta_data JSON NOT NULL DEFAULT '{}'::JSON")
                it.execute("ALTER TABLE e_email_send_transaction ADD COLUMN tags text[] NOT NULL DEFAULT array[]::text[]")
            }
        return -6
    }
}
