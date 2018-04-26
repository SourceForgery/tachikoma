package com.sourceforgery.tachikoma.database.server

import org.avaje.datasource.DataSourcePool
import org.avaje.datasource.PoolStatistics
import org.avaje.datasource.PoolStatus

internal class LoggingDataSourcePool(
    private val originalDataSourcePool: DataSourcePool,
    counter: InvokeCounter
) : LoggingDataSource(
    originalDataSource = originalDataSourcePool,
    counter = counter
), DataSourcePool {

    override fun getName(): String {
        return originalDataSourcePool.name
    }

    override fun isAutoCommit(): Boolean {
        return originalDataSourcePool.isAutoCommit
    }

    override fun shutdown(deregisterDriver: Boolean) {
        originalDataSourcePool.shutdown(deregisterDriver)
    }

    override fun getStatus(reset: Boolean): PoolStatus {
        return originalDataSourcePool.getStatus(reset)
    }

    override fun getStatistics(reset: Boolean): PoolStatistics {
        return originalDataSourcePool.getStatistics(reset)
    }
}
