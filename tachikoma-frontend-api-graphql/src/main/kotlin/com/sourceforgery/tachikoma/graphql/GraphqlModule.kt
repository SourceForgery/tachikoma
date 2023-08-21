package com.sourceforgery.tachikoma.graphql

import com.sourceforgery.tachikoma.graphql.api.Mutations
import com.sourceforgery.tachikoma.graphql.api.Queries
import com.sourceforgery.tachikoma.graphql.api.coercers.AccountIdCoercer
import com.sourceforgery.tachikoma.graphql.api.coercers.MailDomainCoercer
import com.sourceforgery.tachikoma.graphql.api.coercers.URICoercer
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

private val coercers = DI.Module("graphql-coercers") {
    bind<URICoercer>() with instance(URICoercer)
    bind<MailDomainCoercer>() with instance(MailDomainCoercer)
    bind<AccountIdCoercer>() with instance(AccountIdCoercer)
}

val graphqlApiModule = DI.Module("graphql-api") {
    importOnce(coercers)
    bind<Mutations>() with singleton { Mutations(di) }
    bind<Queries>() with singleton { Queries(di) }
    bind<CustomSchemaGeneratorHooks>() with singleton { CustomSchemaGeneratorHooks(di) }
    bind<GraphqlSchemaGenerator>() with singleton { GraphqlSchemaGenerator(di) }
}
