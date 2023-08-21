package com.sourceforgery.tachikoma.graphql.api

import com.expediagroup.graphql.server.operations.Mutation
import com.sourceforgery.tachikoma.graphql.api.account.AccountServiceMutations
import org.kodein.di.DI
import org.kodein.di.DIAware

class Mutations(override val di: DI) : DIAware, Mutation {
    private val accounts = AccountServiceMutations(di)
    fun accounts() = accounts
}
