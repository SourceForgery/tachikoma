package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version5 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.execute("ALTER TABLE e_account ADD COLUMN incoming_mx_domain VARCHAR")
                    it.execute("ALTER TABLE e_account ADD CONSTRAINT incoming_mx_domain UNIQUE (mail_domain)")
                    it.execute("UPDATE e_account SET incoming_mx_domain = mail_domain WHERE incoming_mx_domain IS NULL")
                    it.execute("ALTER TABLE e_account ALTER COLUMN incoming_mx_domain SET NOT NULL")
                }
        return -5
    }
}