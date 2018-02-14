package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version8 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.execute("ALTER TABLE e_email_send_transaction ADD COLUMN bcc text[] NOT NULL DEFAULT array[]::text[]")
                }
        return -8
    }
}