package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version4 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.execute("ALTER TABLE e_incoming_email_address DROP COLUMN mail_domain")
                    it.execute("ALTER TABLE e_incoming_email_address ADD CONSTRAINT uq_e_incoming_email_address_local_part_account_id UNIQUE (local_part, account_id)")
                    it.execute("ALTER TABLE e_incoming_email_address ALTER COLUMN local_part SET NOT NULL")
                    it.execute("ALTER TABLE e_account ADD CONSTRAINT uq_e_account_mail_domain UNIQUE (mail_domain)")
                    it.execute("ALTER TABLE e_incoming_email RENAME COLUMN account_dbo_id TO account_id")
                }
        return -4
    }
}