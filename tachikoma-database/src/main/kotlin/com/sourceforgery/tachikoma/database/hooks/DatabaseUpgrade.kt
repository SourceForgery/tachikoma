package com.sourceforgery.tachikoma.database.hooks

import java.sql.Connection

interface DatabaseUpgrade {
    // Returns the new version of the database
    fun run(connection: Connection): Int
}