package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version3 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
                .createStatement()
                .use {
                    it.executeUpdate("ALTER TABLE e_email_status DROP COLUMN mta_status_code")
                    it.executeUpdate("ALTER TABLE e_email_status ADD COLUMN metaData JSON")
                    it.executeUpdate("CREATE INDEX mail_status_meta_data_mta_status_code ON e_email_status ((metaData->>'mtaStatusCode'))")
                    it.executeUpdate("CREATE INDEX mail_status_meta_data_ip_address ON e_email_status ((metaData->>'ipAddress'))")
                }
        return -2
    }
}