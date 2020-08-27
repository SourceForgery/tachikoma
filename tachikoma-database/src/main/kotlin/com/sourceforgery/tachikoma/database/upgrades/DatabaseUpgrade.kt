package com.sourceforgery.tachikoma.database.upgrades

import java.sql.Connection

interface DatabaseUpgrade {
    val newVersion: Int
    // Returns the new version of the database
    fun run(connection: Connection): Int
}
