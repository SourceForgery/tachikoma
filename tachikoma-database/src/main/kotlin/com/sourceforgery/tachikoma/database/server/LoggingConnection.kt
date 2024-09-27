package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.logging.InvokeCounter
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement

internal class LoggingConnection(
    private val realConnection: Connection,
    private val counter: () -> InvokeCounter,
) : Connection by realConnection {
    @Throws(SQLException::class)
    override fun createStatement(): Statement {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.createStatement()
        } finally {
            counter().inc("createStatement", System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun prepareStatement(sql: String): PreparedStatement {
        return LoggingPreparedStatement(
            preparedStatement = realConnection.prepareStatement(sql),
            sql = sql,
            counter = counter,
        )
    }

    @Throws(SQLException::class)
    override fun prepareCall(sql: String): CallableStatement {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.prepareCall(sql)
        } finally {
            counter().inc(sql, System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun nativeSQL(sql: String): String {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.nativeSQL(sql)
        } finally {
            counter().inc(sql, System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun createStatement(
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): Statement {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.createStatement(resultSetType, resultSetConcurrency)
        } finally {
            counter().inc("createStatement", System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): PreparedStatement {
        return LoggingPreparedStatement(
            preparedStatement = realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql = sql,
            counter = counter,
        )
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
    ): CallableStatement {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency)
        } finally {
            counter().inc(sql, System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int,
    ): PreparedStatement {
        return LoggingPreparedStatement(realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, counter)
    }

    @Throws(SQLException::class)
    override fun prepareCall(
        sql: String,
        resultSetType: Int,
        resultSetConcurrency: Int,
        resultSetHoldability: Int,
    ): CallableStatement {
        val millis = System.currentTimeMillis()
        try {
            return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability)
        } finally {
            counter().inc(sql, System.currentTimeMillis() - millis)
        }
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        autoGeneratedKeys: Int,
    ): PreparedStatement {
        return LoggingPreparedStatement(
            preparedStatement = realConnection.prepareStatement(sql, autoGeneratedKeys),
            sql = sql,
            counter = counter,
        )
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnIndexes: IntArray,
    ): PreparedStatement {
        return LoggingPreparedStatement(
            preparedStatement = realConnection.prepareStatement(sql, columnIndexes),
            sql = sql,
            counter = counter,
        )
    }

    @Throws(SQLException::class)
    override fun prepareStatement(
        sql: String,
        columnNames: Array<String>,
    ): PreparedStatement {
        return LoggingPreparedStatement(
            preparedStatement = realConnection.prepareStatement(sql, columnNames),
            sql = sql,
            counter = counter,
        )
    }
}
