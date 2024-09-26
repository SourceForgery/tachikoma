package com.sourceforgery.tachikoma.graphql.api

import com.expediagroup.graphql.server.operations.Query
import com.sourceforgery.tachikoma.graphql.api.account.AccountServiceQueries
import org.kodein.di.DI
import org.kodein.di.DIAware

class Queries(override val di: DI) : DIAware, Query {
    private val accounts = AccountServiceQueries(di)

    fun accounts() = accounts
}
