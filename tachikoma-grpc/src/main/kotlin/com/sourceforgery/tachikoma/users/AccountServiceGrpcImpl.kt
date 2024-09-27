package com.sourceforgery.tachikoma.users

import com.sourceforgery.tachikoma.account.AccountFacade
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.config.TrackingConfig
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.admin.account.AccountServiceGrpcKt
import com.sourceforgery.tachikoma.grpc.frontend.admin.account.UpdateSettingsRequest
import com.sourceforgery.tachikoma.grpc.frontend.admin.account.UpdateSettingsResponse
import com.sourceforgery.tachikoma.identifiers.MailDomain
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider
import java.net.URI

class AccountServiceGrpcImpl(
    override val di: DI,
) : AccountServiceGrpcKt.AccountServiceCoroutineImplBase(), DIAware {
    private val authentication: () -> Authentication by provider()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val accountFacade: AccountFacade by instance()
    private val trackingConfig: TrackingConfig by instance()

    override suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResponse =
        try {
            val mailDomain = MailDomain(request.mailDomain)
            authentication().requireFrontendAdmin(mailDomain)
            var account = requireNotNull(accountFacade.get(mailDomain)) { "No account with domain $mailDomain" }
            if (request.hasBaseUrl()) {
                account =
                    accountFacade.modifyAccount(
                        mailDomain = mailDomain,
                        baseUrl =
                            request.baseUrl
                                .takeIf { it.isNotEmpty() }
                                ?.let { URI(it) },
                    )
            }
            UpdateSettingsResponse.newBuilder()
                .setMailDomain(account.mailDomain.mailDomain)
                .setBaseUrl(
                    (account.baseUrl ?: trackingConfig.baseUrl)
                        .toASCIIString(),
                )
                .build()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
}
