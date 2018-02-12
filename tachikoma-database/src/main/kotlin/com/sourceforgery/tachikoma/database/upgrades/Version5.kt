package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version5 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.execute("ALTER TABLE e_account ADD COLUMN incoming_mx_domain varchar NOT NULL DEFAULT mail_domain")
                    it.execute("ALTER TABLE e_account ADD CONSTRAINT incoming_mx_domain UNIQUE (mail_domain)")
                }
        return -5
    }
}