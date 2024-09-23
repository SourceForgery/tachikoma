package com.sourceforgery.tachikoma.graphql.api.account

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.sourceforgery.tachikoma.account.AccountFacade
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.identifiers.MailDomain
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider
import java.net.URI

class AccountServiceMutations(override val di: DI) : DIAware {
    private val accountFacade: AccountFacade by instance()
    private val authentication: () -> Authentication by provider()
    private val trackingConfig: TrackingConfig by instance()

    @GraphQLDescription("Change base url of tracking/click urls")
    fun changeBaseUrl(
        mailDomain: MailDomain,
        baseUrl: URI?,
    ): AccountResponse {
        authentication().requireFrontendAdmin(mailDomain)
        val account = accountFacade.modifyAccount(mailDomain, baseUrl)
        return account.toAccountResponse(trackingConfig.baseUrl)
    }
}

class AccountServiceQueries(override val di: DI) : DIAware {
    private val accountFacade: AccountFacade by instance()
    private val authentication: () -> Authentication by provider()
    private val trackingConfig: TrackingConfig by instance()

    @GraphQLDescription("Change base url of tracking/click urls")
    fun getAccountData(mailDomain: MailDomain): AccountResponse {
        authentication().requireFrontendAdmin(mailDomain)
        val account =
            accountFacade[mailDomain]
                ?: error("No account with mailDomain $mailDomain")
        return account.toAccountResponse(trackingConfig.baseUrl)
    }
}
