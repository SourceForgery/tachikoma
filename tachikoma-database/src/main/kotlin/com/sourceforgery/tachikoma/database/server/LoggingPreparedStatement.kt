package com.sourceforgery.tachikoma.database.server

import com.sourceforgery.tachikoma.logging.InvokeCounter
import org.apache.logging.log4j.kotlin.logger
import org.intellij.lang.annotations.MagicConstant
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

internal class LoggingPreparedStatement(
    private val preparedStatement: PreparedStatement,
    private val sql: String,
    private val counter: () -> InvokeCounter,
) : PreparedStatement by preparedStatement {
    private fun inc(
        sql: String?,
        started: Long,
    ) {
        try {
            counter().inc(sql, System.currentTimeMillis() - started)
        } catch (e: Exception) {
            if ("Not inside a request scope" !in (e.message ?: "")) {
                LOGGER.warn(e) { "Unexpected error when logging" }
            }
        }
    }

    @Throws(SQLException::class)
    override fun executeQuery(): ResultSet {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeQuery()
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeUpdate(): Int {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeUpdate()
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun execute(): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.execute()
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun execute(sql: String): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.execute(sql)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun getMoreResults(): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.moreResults
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun getMoreResults(
        @MagicConstant(intValues = [Statement.CLOSE_CURRENT_RESULT.toLong(), Statement.KEEP_CURRENT_RESULT.toLong(), Statement.CLOSE_ALL_RESULTS.toLong()]) current: Int,
    ): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.getMoreResults(current)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String,
        @MagicConstant(intValues = [Statement.RETURN_GENERATED_KEYS.toLong(), Statement.NO_GENERATED_KEYS.toLong()]) autoGeneratedKeys: Int,
    ): Int {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeUpdate(sql, autoGeneratedKeys)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String?,
        columnIndexes: IntArray?,
    ): Int {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeUpdate(sql, columnIndexes)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeUpdate(
        sql: String?,
        columnNames: Array<out String>?,
    ): Int {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeUpdate(sql, columnNames)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        autoGeneratedKeys: Int,
    ): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.execute(sql, autoGeneratedKeys)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        columnIndexes: IntArray,
    ): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.execute(sql, columnIndexes)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun execute(
        sql: String,
        columnNames: Array<out String>?,
    ): Boolean {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.execute(sql, columnNames)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeLargeBatch(): LongArray {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeLargeBatch()
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeLargeUpdate(sql: String?): Long {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeLargeUpdate(sql)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeLargeUpdate(
        sql: String?,
        autoGeneratedKeys: Int,
    ): Long {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeLargeUpdate(sql, autoGeneratedKeys)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeLargeUpdate(
        sql: String?,
        columnIndexes: IntArray?,
    ): Long {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeLargeUpdate(sql, columnIndexes)
        } finally {
            inc(sql, millis)
        }
    }

    @Throws(SQLException::class)
    override fun executeLargeUpdate(
        sql: String?,
        columnNames: Array<out String>?,
    ): Long {
        val millis = System.currentTimeMillis()
        try {
            return preparedStatement.executeLargeUpdate(sql, columnNames)
        } finally {
            inc(sql, millis)
        }
    }

    companion object {
        private val LOGGER = logger()
    }
}
