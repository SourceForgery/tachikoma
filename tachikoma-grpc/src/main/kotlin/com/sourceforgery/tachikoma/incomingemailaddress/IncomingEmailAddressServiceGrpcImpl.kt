package com.sourceforgery.tachikoma.incomingemailaddress

import com.google.protobuf.Empty
import com.sourceforgery.tachikoma.auth.Authentication
import com.sourceforgery.tachikoma.grpc.catcher.GrpcExceptionMap
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddress
import com.sourceforgery.tachikoma.grpc.frontend.incomingemailaddress.IncomingEmailAddressServiceGrpcKt
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.provider

internal class IncomingEmailAddressServiceGrpcImpl(
    override val di: DI
) : IncomingEmailAddressServiceGrpcKt.IncomingEmailAddressServiceCoroutineImplBase(), DIAware {

    private val incomingEmailAddressService: IncomingEmailAddressService by instance()
    private val grpcExceptionMap: GrpcExceptionMap by instance()
    private val authentication: () -> Authentication by provider()

    override suspend fun addIncomingEmailAddress(request: IncomingEmailAddress): Empty =
        try {
            val auth = authentication()
            auth.requireFrontendAdmin(auth.mailDomain)
            incomingEmailAddressService.addIncomingEmailAddress(request, auth.authenticationId)
            Empty.getDefaultInstance()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }

    override fun getIncomingEmailAddresses(request: Empty) = flow<IncomingEmailAddress> {
        val auth = authentication()
        auth.requireFrontendAdmin(auth.mailDomain)
        incomingEmailAddressService.getIncomingEmailAddresses(auth.authenticationId)
    }.catch { throw grpcExceptionMap.findAndConvertAndLog(it) }

    override suspend fun deleteIncomingEmailAddress(request: IncomingEmailAddress): Empty =
        try {
            val auth = authentication()
            auth.requireFrontendAdmin(auth.mailDomain)
            incomingEmailAddressService.deleteIncomingEmailAddress(request, auth.authenticationId)
            Empty.getDefaultInstance()
        } catch (e: Exception) {
            throw grpcExceptionMap.findAndConvertAndLog(e)
        }
}
