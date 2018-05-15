package com.sourceforgery.tachikoma.database.server

import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

internal open class LoggingDataSource(
    private val originalDataSource: DataSource,
    private val counter: InvokeCounter
) : DataSource by originalDataSource {

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        return LoggingConnection(
            realConnection = originalDataSource.connection,
            counter = counter
        )
    }

    @Throws(SQLException::class)
    override fun getConnection(username: String, password: String): Connection {
        return LoggingConnection(
            realConnection = originalDataSource.getConnection(username, password),
            counter = counter
        )
    }
}
