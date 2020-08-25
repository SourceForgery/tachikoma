package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.logging.InvokeCounter
import io.ebean.datasource.DataSourcePool
import java.sql.Connection
import java.sql.SQLException

internal class LoggingDataSourcePool(
    private val originalDataSourcePool: DataSourcePool,
    private val counter: InvokeCounter
) : DataSourcePool by originalDataSourcePool {

    @Throws(SQLException::class)
    override fun getConnection(): Connection {
        return LoggingConnection(
            realConnection = originalDataSourcePool.connection,
            counter = counter
        )
    }

    @Throws(SQLException::class)
    override fun getConnection(username: String, password: String): Connection {
        return LoggingConnection(
            realConnection = originalDataSourcePool.getConnection(username, password),
            counter = counter
        )
    }
}
