package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

class Version9 : DatabaseUpgrade {
    override fun run(connection: Connection): Int {
        connection
            .createStatement()
            .use {
                it.execute("ALTER TABLE e_account DROP COLUMN incoming_mx_domain")
            }
        return -9
    }
}